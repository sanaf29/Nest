package com.nest.dto.response;

import com.nest.model.GoalStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Everything the dashboard needs for one month. Amounts are always grouped by currency, never mixed. */
public record DashboardSummaryResponse(
        int year,
        int month,
        String preferredCurrency,
        @Schema(description = "Total of open accounts per currency (negative = net debt)") List<CurrencyBalance> balances,
        @Schema(description = "Income, expenses, and net cash flow for the month, per currency") List<CashFlow> cashFlow,
        @Schema(description = "This month's budget in the preferred currency, or null if none") BudgetSnapshot budget,
        List<GoalProgress> activeGoals,
        long achievedGoalCount
) {
    public record CurrencyBalance(String currency, BigDecimal total, int accountCount) {
    }

    public record CashFlow(String currency, BigDecimal income, BigDecimal expenses, BigDecimal net) {
    }

    public record BudgetSnapshot(Long budgetId, String currency, BigDecimal spendable, BigDecimal spent,
                                 BigDecimal remaining, BigDecimal percentUsed, int categoriesNearLimit,
                                 int categoriesExceeded) {
    }

    public record GoalProgress(Long id, String name, String currency, BigDecimal savedAmount, BigDecimal targetAmount,
                               BigDecimal progressPercent, LocalDate targetDate, GoalStatus status) {
    }
}
