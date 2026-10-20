package com.nest.service.insight;

import java.util.Optional;

/**
 * One deterministic, explainable rule (Strategy pattern). Implementations are Spring beans; the
 * {@link InsightEngine} receives all of them, so adding a rule never changes existing code.
 */
public interface FinancialInsightRule {

    Optional<FinancialInsight> evaluate(FinancialSummary summary);
}
