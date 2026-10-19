package com.nest.model;

import java.util.Map;
import java.util.Objects;

/**
 * What actually happened in one month and currency: total income and expenses per category.
 * Transfers are never included. Produced by a query and consumed by pure calculations
 * ({@link MonthlyBudget#summarize}), which keeps those calculations testable without a database.
 */
public record MonthlyActuals(Money income, Map<Long, Money> expensesByCategory) {

    public MonthlyActuals {
        Objects.requireNonNull(income, "income");
        expensesByCategory = Map.copyOf(expensesByCategory);
    }

    public static MonthlyActuals none(String currency) {
        return new MonthlyActuals(Money.zero(currency), Map.of());
    }

    public Money totalExpenses() {
        return expensesByCategory.values().stream().reduce(Money.zero(income.currency()), Money::plus);
    }

    public Money expensesIn(long categoryId) {
        return expensesByCategory.getOrDefault(categoryId, Money.zero(income.currency()));
    }

    public Money netCashFlow() {
        return income.minus(totalExpenses());
    }
}
