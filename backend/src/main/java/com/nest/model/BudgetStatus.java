package com.nest.model;

import java.math.BigDecimal;

/** How a category's spending compares with its limit. */
public enum BudgetStatus {
    ON_TRACK,
    NEAR_LIMIT,
    EXCEEDED;

    /** Percentage of the limit from which a category is reported as {@link #NEAR_LIMIT}. */
    public static final BigDecimal NEAR_LIMIT_PERCENT = BigDecimal.valueOf(80);

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public static BudgetStatus of(BigDecimal percentUsed) {
        if (percentUsed.compareTo(HUNDRED) > 0) {
            return EXCEEDED;
        }
        return percentUsed.compareTo(NEAR_LIMIT_PERCENT) >= 0 ? NEAR_LIMIT : ON_TRACK;
    }
}
