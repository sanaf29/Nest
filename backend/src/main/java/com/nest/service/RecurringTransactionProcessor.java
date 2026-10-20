package com.nest.service;

import com.nest.exception.BusinessRuleException;
import com.nest.model.RecurringTransaction;
import com.nest.repository.RecurringTransactionRepository;
import com.nest.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;

/**
 * Generates due occurrences of recurring transactions, exactly once.
 *
 * <p>How duplicates are prevented:
 * <ol>
 *   <li>Each rule is processed in its <em>own</em> database transaction, so one failing rule never
 *       rolls back another.</li>
 *   <li>Inside that transaction, the occurrence is inserted and {@code nextOccurrence} advanced
 *       together: either both commit or neither does.</li>
 *   <li>If two runs race on the same rule, the loser fails on the rule's {@code @Version} or on the
 *       unique (rule, occurrence date) index, and its work is rolled back.</li>
 * </ol>
 * All state lives in the database, so restarts are safe; missed occurrences are caught up,
 * at most {@value #MAX_OCCURRENCES_PER_RUN} per rule per run.
 */
@Component
public class RecurringTransactionProcessor {

    public static final int MAX_OCCURRENCES_PER_RUN = 60;
    private static final Logger log = LoggerFactory.getLogger(RecurringTransactionProcessor.class);

    private final RecurringTransactionRepository ruleRepository;
    private final TransactionRepository transactionRepository;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate perRule;

    public RecurringTransactionProcessor(RecurringTransactionRepository ruleRepository,
                                         TransactionRepository transactionRepository,
                                         ApplicationEventPublisher events,
                                         PlatformTransactionManager transactionManager) {
        this.ruleRepository = ruleRepository;
        this.transactionRepository = transactionRepository;
        this.events = events;
        this.perRule = new TransactionTemplate(transactionManager);
        this.perRule.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Totals for one run. */
    public record Result(int generated, int paused) {
        Result plus(Result other) {
            return new Result(generated + other.generated, paused + other.paused);
        }
    }

    /** Every user's due rules (used by the scheduler). */
    public Result processDue(LocalDate today) {
        return process(ruleRepository.findDueIds(today), today);
    }

    /** One user's due rules (used by "run now"). */
    public Result processDueForUser(long userId, LocalDate today) {
        return process(ruleRepository.findDueIdsForUser(userId, today), today);
    }

    private Result process(List<Long> ruleIds, LocalDate today) {
        Result total = new Result(0, 0);
        for (Long id : ruleIds) {
            total = total.plus(processRule(id, today));
        }
        return total;
    }

    private Result processRule(long ruleId, LocalDate today) {
        try {
            Result result = perRule.execute(status -> generateFor(ruleId, today));
            return result == null ? new Result(0, 0) : result;
        } catch (ObjectOptimisticLockingFailureException | DataIntegrityViolationException concurrent) {
            // Another run processed this rule at the same moment; its result stands.
            log.info("Recurring rule {} was processed concurrently; skipping", ruleId);
            return new Result(0, 0);
        }
    }

    private Result generateFor(long ruleId, LocalDate today) {
        RecurringTransaction rule = ruleRepository.findById(ruleId).orElse(null);
        if (rule == null || !rule.isDue(today)) {
            return new Result(0, 0);
        }
        int generated = 0;
        try {
            while (rule.isDue(today) && generated < MAX_OCCURRENCES_PER_RUN) {
                transactionRepository.save(rule.generateNextOccurrence());
                generated++;
            }
        } catch (BusinessRuleException unusable) {
            // The domain validated before changing anything, so pausing here leaves a consistent rule.
            String reason = "Paused automatically: " + unusable.getMessage();
            rule.pause(reason);
            events.publishEvent(new RecurringPausedEvent(rule.getAccount().ownerId(), ruleId, rule.getDescription(),
                    reason, today));
            ruleRepository.saveAndFlush(rule);
            return new Result(generated, 1);
        }
        ruleRepository.saveAndFlush(rule);
        return new Result(generated, 0);
    }
}
