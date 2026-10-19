package com.nest.dto.response;

import com.nest.model.BudgetStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

public record BudgetSummaryResponse(
        Long budgetId,
        int year,
        int month,
        String currency,
        BigDecimal plannedIncome,
        BigDecimal savingsTarget,
        BigDecimal spendableAmount,
        BigDecimal allocatedAmount,
        BigDecimal actualIncome,
        BigDecimal actualExpenses,
        @Schema(description = "Actual income − actual expenses") BigDecimal netCashFlow,
        @Schema(description = "Spendable − actual expenses; negative when overspent") BigDecimal remaining,
        @Schema(description = "Actual expenses as % of spendable; null if nothing is spendable") BigDecimal percentUsed,
        @Schema(description = "Net cash flow as % of the savings target; null if the target is 0") BigDecimal savingsProgressPercent,
        @Schema(description = "Expenses in categories without a limit") BigDecimal unbudgetedSpending,
        List<CategoryLine> categories
) {
    public record CategoryLine(Long categoryId, String categoryName, String color, BigDecimal limit, BigDecimal spent,
                               BigDecimal remaining, BigDecimal percentUsed, BudgetStatus status) {
    }
}
