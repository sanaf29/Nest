package com.nest.service;

import com.nest.model.BudgetSummary;
import com.nest.model.Money;
import com.nest.model.MonthlyBudget;
import com.nest.model.NotificationCategory;
import com.nest.model.RecurringTransaction;
import com.nest.model.Reminder;
import com.nest.model.TransactionType;
import com.nest.model.User;
import com.nest.repository.MonthlyBudgetRepository;
import com.nest.repository.RecurringTransactionRepository;
import com.nest.repository.ReminderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Turns reminders and upcoming bills into notifications. Every item is handled in its own
 * transaction, and every notification has a deterministic dedup key, so the job can run every
 * minute, overlap with itself, or restart at any point without notifying twice.
 */
@Component
public class ReminderDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ReminderDispatcher.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.CANADA);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' HH:mm 'UTC'", Locale.CANADA)
            .withZone(ZoneOffset.UTC);

    private final ReminderRepository reminderRepository;
    private final RecurringTransactionRepository recurringRepository;
    private final MonthlyBudgetRepository budgetRepository;
    private final LedgerAnalytics ledgerAnalytics;
    private final Notifier notifier;
    private final TransactionTemplate perItem;

    public ReminderDispatcher(ReminderRepository reminderRepository, RecurringTransactionRepository recurringRepository,
                              MonthlyBudgetRepository budgetRepository, LedgerAnalytics ledgerAnalytics, Notifier notifier,
                              PlatformTransactionManager transactionManager) {
        this.reminderRepository = reminderRepository;
        this.recurringRepository = recurringRepository;
        this.budgetRepository = budgetRepository;
        this.ledgerAnalytics = ledgerAnalytics;
        this.notifier = notifier;
        this.perItem = new TransactionTemplate(transactionManager);
        this.perItem.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Number of notifications created by one pass. */
    public record Result(int timed, int budget, int recurring) {
        public int total() {
            return timed + budget + recurring;
        }
    }

    public Result runAll(Instant now) {
        return new Result(dispatchTimedReminders(now), evaluateBudgetThresholds(now), notifyUpcomingRecurring(now));
    }

    /** One-shot reminders whose time has come: notify, then mark SENT, atomically. */
    public int dispatchTimedReminders(Instant now) {
        int created = 0;
        for (Long id : reminderRepository.findDueIds(now)) {
            created += safely(() -> {
                Reminder reminder = reminderRepository.findById(id).orElse(null);
                if (reminder == null || !reminder.isDue(now)) {
                    return 0;
                }
                boolean isNew = notifier.notifyOnce(reminder.getUser(), NotificationCategory.REMINDER,
                        reminder.getType().label() + ": " + reminder.getTitle(), timedMessage(reminder),
                        "/reminders", "reminder:" + id);
                reminder.markSent(now);
                return isNew ? 1 : 0;
            });
        }
        return created;
    }

    private static String timedMessage(Reminder r) {
        StringBuilder message = new StringBuilder();
        switch (r.getType()) {
            case PARKING -> message.append("Your parking at ").append(r.getLocation()).append(" expires ")
                    .append(DATE_TIME.format(r.getDueAt())).append('.');
            case SAVINGS_GOAL_DEADLINE -> message.append("'").append(r.getSavingsGoal().getName()).append("' has ")
                    .append(r.getSavingsGoal().savedAmount()).append(" of ").append(r.getSavingsGoal().target())
                    .append(" saved; its deadline is ").append(DATE_TIME.format(r.getDueAt())).append('.');
            default -> message.append("Due ").append(DATE_TIME.format(r.getDueAt())).append('.');
        }
        if (r.getNotes() != null) {
            message.append(' ').append(r.getNotes());
        }
        return message.toString();
    }

    /**
     * Budget alerts: compare this month's spending (overall, or for one category) with the
     * threshold. At most one notification per reminder per month.
     */
    public int evaluateBudgetThresholds(Instant now) {
        YearMonth month = YearMonth.from(now.atZone(ZoneOffset.UTC));
        int created = 0;
        for (Reminder candidate : reminderRepository.findActiveBudgetThresholds()) {
            long id = candidate.getId();
            created += safely(() -> {
                Reminder reminder = reminderRepository.findById(id).orElseThrow();
                User user = reminder.getUser();
                String currency = user.getPreferredCurrency();
                Optional<MonthlyBudget> budget = budgetRepository.findByUserIdAndYearAndMonthAndCurrency(
                        user.getId(), month.getYear(), month.getMonthValue(), currency);
                if (budget.isEmpty()) {
                    return 0;
                }
                BudgetSummary summary = budget.get().summarize(ledgerAnalytics.monthlyActuals(user.getId(), month, currency));
                Optional<Usage> usage = usageFor(reminder, summary);
                if (usage.isEmpty() || usage.get().percent().compareTo(BigDecimal.valueOf(reminder.getThresholdPercent())) < 0) {
                    return 0;
                }
                Usage u = usage.get();
                return notifier.notifyOnce(user, NotificationCategory.BUDGET,
                        "%s has reached %d%% of its budget".formatted(u.label(), reminder.getThresholdPercent()),
                        "%s: %s spent of %s (%s%%).".formatted(u.label(), u.spent(), u.limit(), u.percent().stripTrailingZeros().toPlainString()),
                        "/budget", "budget:%d:%s".formatted(id, month)) ? 1 : 0;
            });
        }
        return created;
    }

    private record Usage(String label, Money spent, Money limit, BigDecimal percent) {
    }

    private static Optional<Usage> usageFor(Reminder reminder, BudgetSummary summary) {
        if (reminder.getCategory() == null) {
            return summary.percentUsed() == null ? Optional.empty()
                    : Optional.of(new Usage("Your monthly budget", summary.actualExpenses(), summary.spendable(), summary.percentUsed()));
        }
        return summary.categories().stream()
                .filter(line -> line.category().getId().equals(reminder.getCategory().getId()))
                .findFirst()
                .map(line -> new Usage(line.category().getName(), line.spent(), line.limit(), line.percentUsed()));
    }

    /** "Bill due soon" notices for recurring rules with {@code remindDaysBefore}. */
    public int notifyUpcomingRecurring(Instant now) {
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        int created = 0;
        for (RecurringTransaction rule : recurringRepository.findWithRemindersDueBetween(today, today.plusDays(30))) {
            LocalDate due = rule.getNextOccurrence();
            if (due.minusDays(rule.getRemindDaysBefore()).isAfter(today)) {
                continue;
            }
            created += safely(() -> notifier.notifyOnce(rule.getAccount().getUser(), NotificationCategory.RECURRING,
                    "%s is due %s".formatted(rule.getDescription(), due.equals(today) ? "today" : "on " + DATE.format(due)),
                    "%s of %s will be recorded on %s.".formatted(rule.getType() == TransactionType.INCOME ? "Income" : "A payment",
                            rule.amount(), DATE.format(due)),
                    "/recurring", "recurring-due:%d:%s".formatted(rule.getId(), due)) ? 1 : 0);
        }
        return created;
    }

    private int safely(Supplier<Integer> work) {
        try {
            Integer result = perItem.execute(status -> work.get());
            return result == null ? 0 : result;
        } catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException concurrent) {
            log.info("Notification already created by a concurrent run; skipping");
            return 0;
        }
    }
}
