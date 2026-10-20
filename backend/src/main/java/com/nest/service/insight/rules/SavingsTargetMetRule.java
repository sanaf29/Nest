package com.nest.service.insight.rules;

import com.nest.model.BudgetSummary;
import com.nest.service.insight.FinancialInsight;
import com.nest.service.insight.FinancialInsightRule;
import com.nest.service.insight.FinancialSummary;
import com.nest.service.insight.InsightSeverity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/** Positive feedback: this month's net cash flow already covers the budget's savings target. */
@Component
public class SavingsTargetMetRule implements FinancialInsightRule {

    @Override
    public Optional<FinancialInsight> evaluate(FinancialSummary summary) {
        Optional<BudgetSummary> budget = summary.budget();
        if (budget.isEmpty() || !budget.get().savingsTarget().isPositive()) {
            return Optional.empty();
        }
        BigDecimal progress = budget.get().savingsProgressPercent();
        if (progress == null || progress.compareTo(BigDecimal.valueOf(100)) < 0) {
            return Optional.empty();
        }
        return Optional.of(new FinancialInsight("SAVINGS_TARGET_MET", InsightSeverity.POSITIVE,
                "You've reached this month's savings target",
                "Income minus expenses is %s, covering your savings target of %s."
                        .formatted(budget.get().netCashFlow(), budget.get().savingsTarget())));
    }
}
