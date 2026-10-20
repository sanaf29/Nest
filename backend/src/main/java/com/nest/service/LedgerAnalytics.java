package com.nest.service;

import com.nest.model.Money;
import com.nest.model.MonthlyActuals;
import com.nest.model.TransactionType;
import com.nest.repository.CategoryTotal;
import com.nest.repository.TransactionRepository;
import com.nest.repository.TypeTotal;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns database aggregates into domain values for budgets, dashboards, and insights.
 *
 * <p>Rules applied here, in one place:
 * <ul>
 *   <li>A transaction belongs to the month of its {@code transactionDate}; the month is the
 *       inclusive range [first day, last day] from {@link YearMonth}, so 28/29/30/31-day months
 *       and leap years are handled.</li>
 *   <li>Only INCOME and EXPENSE count. Transfers move money but are neither.</li>
 *   <li>Only accounts in the requested currency count; currencies are never mixed.</li>
 * </ul>
 * This is a concrete helper rather than an interface: it has one implementation and no
 * reason to vary.
 */
@Component
public class LedgerAnalytics {

    public static final Set<TransactionType> CASH_FLOW_TYPES = Set.of(TransactionType.INCOME, TransactionType.EXPENSE);

    private final TransactionRepository transactionRepository;

    public LedgerAnalytics(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public MonthlyActuals monthlyActuals(long userId, YearMonth month, String currency) {
        List<TypeTotal> totals = transactionRepository.sumByType(userId, currency,
                month.atDay(1), month.atEndOfMonth(), Set.of(TransactionType.INCOME));
        Money income = totals.stream()
                .map(total -> Money.of(total.total(), currency))
                .reduce(Money.zero(currency), Money::plus);

        Map<Long, Money> expenses = new HashMap<>();
        for (CategoryTotal row : expensesByCategory(userId, month, currency)) {
            expenses.put(row.categoryId(), Money.of(row.total(), currency));
        }
        return new MonthlyActuals(income, expenses);
    }

    @Transactional(readOnly = true)
    public List<CategoryTotal> expensesByCategory(long userId, YearMonth month, String currency) {
        return transactionRepository.sumByCategory(userId, currency, month.atDay(1), month.atEndOfMonth(),
                TransactionType.EXPENSE);
    }

    /** Total expenses in a currency over an inclusive date range. */
    @Transactional(readOnly = true)
    public Money totalExpenses(long userId, String currency, LocalDate from, LocalDate to) {
        return transactionRepository.sumByType(userId, currency, from, to, Set.of(TransactionType.EXPENSE)).stream()
                .map(total -> Money.of(total.total(), currency))
                .reduce(Money.zero(currency), Money::plus);
    }
}
