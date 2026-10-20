package com.nest.service.insight;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Runs every rule against a summary and orders the results most urgent first. */
@Component
public class InsightEngine {

    private final List<FinancialInsightRule> rules;

    public InsightEngine(List<FinancialInsightRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public List<FinancialInsight> evaluate(FinancialSummary summary) {
        return rules.stream()
                .map(rule -> rule.evaluate(summary))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(FinancialInsight::severity).thenComparing(FinancialInsight::code))
                .toList();
    }
}
