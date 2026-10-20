package com.nest.service.insight.rules;

import com.nest.service.insight.FinancialInsight;
import com.nest.service.insight.FinancialInsightRule;
import com.nest.service.insight.FinancialSummary;
import com.nest.service.insight.InsightSeverity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** A recurring bill whose latest charge is higher than the one before it (e.g. a subscription price rise). */
@Component
public class RecurringExpenseIncreaseRule implements FinancialInsightRule {

    @Override
    public Optional<FinancialInsight> evaluate(FinancialSummary summary) {
        List<FinancialSummary.RecurringChange> increases = summary.recurringChanges().stream()
                .filter(c -> c.latestAmount().amount().compareTo(c.previousAmount().amount()) > 0)
                .toList();
        if (increases.isEmpty()) {
            return Optional.empty();
        }
        String detail = increases.stream()
                .map(c -> "%s went from %s to %s (+%s)".formatted(c.description(), c.previousAmount(), c.latestAmount(),
                        Percentages.whole(Percentages.increase(c.latestAmount(), c.previousAmount()))))
                .collect(Collectors.joining("; "));
        String title = increases.size() == 1
                ? "%s costs more than before".formatted(increases.getFirst().description())
                : "%d recurring expenses went up".formatted(increases.size());
        return Optional.of(new FinancialInsight("RECURRING_EXPENSE_INCREASED", InsightSeverity.INFO, title, detail + "."));
    }
}
