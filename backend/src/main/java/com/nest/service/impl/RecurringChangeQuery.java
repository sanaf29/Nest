package com.nest.service.impl;

import com.nest.model.Money;
import com.nest.model.Transaction;
import com.nest.repository.TransactionRepository;
import com.nest.service.insight.FinancialSummary;
import com.nest.service.insight.RecurringChangeSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Compares the two most recent generated amounts of each recurring expense (last ~13 months). */
@Component
public class RecurringChangeQuery implements RecurringChangeSource {

    private final TransactionRepository transactionRepository;
    private final Clock clock;

    public RecurringChangeQuery(TransactionRepository transactionRepository, Clock clock) {
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FinancialSummary.RecurringChange> recentChanges(long userId, String currency) {
        LocalDate since = LocalDate.now(clock).minusMonths(13);
        Map<Long, List<Transaction>> byRule = new LinkedHashMap<>();
        for (Transaction t : transactionRepository.findRecurringExpenseOccurrences(userId, currency, since)) {
            byRule.computeIfAbsent(t.getRecurringTransaction().getId(), id -> new ArrayList<>()).add(t);
        }
        List<FinancialSummary.RecurringChange> changes = new ArrayList<>();
        for (List<Transaction> occurrences : byRule.values()) {
            if (occurrences.size() < 2) continue;
            Money latest = occurrences.get(0).money();
            Money previous = occurrences.get(1).money();
            if (latest.amount().compareTo(previous.amount()) != 0) {
                changes.add(new FinancialSummary.RecurringChange(occurrences.get(0).getRecurringTransaction().getDescription(),
                        previous, latest));
            }
        }
        return changes;
    }
}
