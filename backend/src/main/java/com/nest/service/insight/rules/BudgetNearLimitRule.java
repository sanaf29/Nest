package com.nest.service.insight.rules;

import com.nest.model.BudgetStatus;
import com.nest.model.BudgetSummary;
import com.nest.service.insight.FinancialInsight;
import com.nest.service.insight.FinancialInsightRule;
import com.nest.service.insight.FinancialSummary;
import com.nest.service.insight.InsightSeverity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Categories at 80–100 % of their limit: still within budget, but close. */
@Component
public class BudgetNearLimitRule implements FinancialInsightRule {

    @Override
    public Optional<FinancialInsight> evaluate(FinancialSummary summary) {
        List<BudgetSummary.CategoryLine> near = summary.budget().stream()
                .flatMap(b -> b.categories().stream())
                .filter(line -> line.status() == BudgetStatus.NEAR_LIMIT)
                .toList();
        if (near.isEmpty()) {
            return Optional.empty();
        }
        String detail = near.stream()
                .map(l -> "%s has %s left (%s used)".formatted(l.category().getName(), l.remaining(),
                        Percentages.whole(l.percentUsed())))
                .collect(Collectors.joining("; "));
        String title = near.size() == 1
                ? "%s is close to its limit".formatted(near.getFirst().category().getName())
                : "%d categories are close to their limits".formatted(near.size());
        return Optional.of(new FinancialInsight("BUDGET_NEAR_LIMIT", InsightSeverity.WARNING, title, detail + "."));
    }
}
