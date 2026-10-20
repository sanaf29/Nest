package com.nest.service.insight.rules;

import com.nest.model.Money;
import com.nest.service.insight.FinancialInsight;
import com.nest.service.insight.FinancialInsightRule;
import com.nest.service.insight.FinancialSummary;
import com.nest.service.insight.InsightSeverity;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** More went out than came in this month. Transfers are not counted either way. */
@Component
public class HighSpendingRule implements FinancialInsightRule {

    @Override
    public Optional<FinancialInsight> evaluate(FinancialSummary summary) {
        Money income = summary.income();
        Money expenses = summary.expenses();
        if (!income.isPositive() || expenses.amount().compareTo(income.amount()) <= 0) {
            return Optional.empty();
        }
        return Optional.of(new FinancialInsight("SPENDING_EXCEEDS_INCOME", InsightSeverity.WARNING,
                "You're spending more than you earn this month",
                "Expenses of %s are %s more than your income of %s.".formatted(expenses, expenses.minus(income), income)));
    }
}
