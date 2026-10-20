package com.nest.service.insight;

import java.util.Objects;

/**
 * One explainable observation. {@code message} always states the numbers that triggered it, so
 * the user can see why it appeared.
 *
 * @param code stable identifier (e.g. {@code BUDGET_EXCEEDED}) for clients and tests
 */
public record FinancialInsight(String code, InsightSeverity severity, String title, String message) {

    public FinancialInsight {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(message, "message");
    }
}
