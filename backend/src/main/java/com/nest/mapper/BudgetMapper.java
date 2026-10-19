package com.nest.mapper;

import com.nest.dto.response.BudgetResponse;
import com.nest.dto.response.BudgetSummaryResponse;
import com.nest.model.BudgetAllocation;
import com.nest.model.BudgetSummary;
import com.nest.model.MonthlyBudget;
import org.springframework.stereotype.Component;

@Component
public class BudgetMapper {

    public BudgetResponse toResponse(MonthlyBudget budget) {
        return new BudgetResponse(
                budget.getId(),
                budget.period().getYear(),
                budget.period().getMonthValue(),
                budget.getCurrency(),
                budget.plannedIncome().amount(),
                budget.getSavingsStrategy(),
                budget.getSavingsValue(),
                budget.savingsTarget().amount(),
                budget.spendable().amount(),
                budget.allocatedTotal().amount(),
                budget.spendable().minus(budget.allocatedTotal()).amount(),
                budget.getAllocations().stream().map(BudgetMapper::toAllocation).toList(),
                budget.getCreatedAt(),
                budget.getUpdatedAt());
    }

    private static BudgetResponse.Allocation toAllocation(BudgetAllocation allocation) {
        return new BudgetResponse.Allocation(
                allocation.getId(),
                allocation.getCategory().getId(),
                allocation.getCategory().getName(),
                allocation.getCategory().getColor(),
                allocation.getCategory().isArchived(),
                allocation.limit().amount());
    }

    public BudgetSummaryResponse toSummaryResponse(MonthlyBudget budget, BudgetSummary summary) {
        return new BudgetSummaryResponse(
                budget.getId(),
                budget.period().getYear(),
                budget.period().getMonthValue(),
                budget.getCurrency(),
                summary.plannedIncome().amount(),
                summary.savingsTarget().amount(),
                summary.spendable().amount(),
                summary.allocated().amount(),
                summary.actualIncome().amount(),
                summary.actualExpenses().amount(),
                summary.netCashFlow().amount(),
                summary.remaining().amount(),
                summary.percentUsed(),
                summary.savingsProgressPercent(),
                summary.unbudgetedSpending().amount(),
                summary.categories().stream()
                        .map(line -> new BudgetSummaryResponse.CategoryLine(
                                line.category().getId(),
                                line.category().getName(),
                                line.category().getColor(),
                                line.limit().amount(),
                                line.spent().amount(),
                                line.remaining().amount(),
                                line.percentUsed(),
                                line.status()))
                        .toList());
    }
}
