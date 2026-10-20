package com.nest.service.insight.rules;

import com.nest.model.BudgetStatus;
import com.nest.model.BudgetSummary;
import com.nest.service.insight.FinancialInsight;
import com.nest.service.insight.FinancialInsightRule;
import com.nest.service.insight.FinancialSummary;
import com.nest.service.insight.InsightSeverity;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Categories whose spending is above their budget limit (> 100 %). */
@Component
public class BudgetExceededRule implements FinancialInsightRule {

    @Override
    public Optional<FinancialInsight> evaluate(FinancialSummary summary) {
        List<BudgetSummary.CategoryLine> over = summary.budget().stream()
                .flatMap(b -> b.categories().stream())
                .filter(line -> line.status() == BudgetStatus.EXCEEDED)
                .sorted(Comparator.comparing(BudgetSummary.CategoryLine::percentUsed).reversed())
                .toList();
        if (over.isEmpty()) {
            return Optional.empty();
        }
        String detail = over.stream()
                .map(l -> "%s: %s of %s (%s)".formatted(l.category().getName(), l.spent(), l.limit(),
                        Percentages.whole(l.percentUsed())))
                .collect(Collectors.joining("; "));
        String title = over.size() == 1
                ? "%s is over budget".formatted(over.getFirst().category().getName())
                : "%d categories are over budget".formatted(over.size());
        return Optional.of(new FinancialInsight("BUDGET_EXCEEDED", InsightSeverity.ALERT, title,
                "Spending has passed its limit: " + detail + "."));
    }
}
