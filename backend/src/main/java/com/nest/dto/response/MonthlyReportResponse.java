package com.nest.dto.response;

import com.nest.model.BudgetStatus;
import com.nest.model.GoalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Everything needed for a printable monthly report, in one currency. */
public record MonthlyReportResponse(
        int year,
        int month,
        String currency,
        Totals totals,
        Totals previousMonth,
        List<CategoryAmount> expensesByCategory,
        List<CategoryAmount> incomeByCategory,
        Budget budget,
        List<Goal> goals,
        List<TopExpense> topExpenses
) {
    public record Totals(BigDecimal income, BigDecimal expenses, BigDecimal net) {
    }

    public record CategoryAmount(String category, String color, BigDecimal amount, BigDecimal percentOfTotal) {
    }

    public record Budget(BigDecimal plannedIncome, BigDecimal savingsTarget, BigDecimal spendable, BigDecimal spent,
                         BigDecimal remaining, List<BudgetLine> categories) {
    }

    public record BudgetLine(String category, BigDecimal limit, BigDecimal spent, BigDecimal percentUsed, BudgetStatus status) {
    }

    public record Goal(String name, String currency, BigDecimal saved, BigDecimal target, BigDecimal progressPercent, GoalStatus status) {
    }

    public record TopExpense(LocalDate date, String description, String category, String account, BigDecimal amount) {
    }
}
