package com.nest.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Plan-versus-actual for one monthly budget. Immutable result of {@link MonthlyBudget#summarize}.
 *
 * @param spendable             planned income − savings target
 * @param remaining             spendable − actual expenses (negative when overspent)
 * @param percentUsed           actual expenses ÷ spendable × 100, or {@code null} when nothing is spendable
 * @param savingsProgressPercent net cash flow ÷ savings target × 100, or {@code null} when the target is 0
 * @param unbudgetedSpending    expenses in categories without an allocation
 */
public record BudgetSummary(
        Money plannedIncome,
        Money savingsTarget,
        Money spendable,
        Money allocated,
        Money actualIncome,
        Money actualExpenses,
        Money netCashFlow,
        Money remaining,
        BigDecimal percentUsed,
        BigDecimal savingsProgressPercent,
        Money unbudgetedSpending,
        List<CategoryLine> categories
) {

    /** One allocation compared with its spending. */
    public record CategoryLine(Category category, Money limit, Money spent, Money remaining,
                               BigDecimal percentUsed, BudgetStatus status) {
    }

    /** part ÷ whole × 100, rounded HALF_EVEN to 2 decimals; {@code null} when whole is zero. */
    static BigDecimal percent(Money part, Money whole) {
        if (whole.amount().signum() == 0) {
            return null;
        }
        return part.amount().multiply(BigDecimal.valueOf(100))
                .divide(whole.amount(), 2, RoundingMode.HALF_EVEN);
    }
}
