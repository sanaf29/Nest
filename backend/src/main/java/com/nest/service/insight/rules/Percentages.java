package com.nest.service.insight.rules;

import com.nest.model.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Shared formatting for rule messages. */
final class Percentages {

    private Percentages() {
    }

    /** (a − b) ÷ b × 100, rounded to a whole percent, HALF_EVEN. */
    static BigDecimal increase(Money current, Money previous) {
        return current.amount().subtract(previous.amount()).multiply(BigDecimal.valueOf(100))
                .divide(previous.amount(), 0, RoundingMode.HALF_EVEN);
    }

    static String whole(BigDecimal percent) {
        return percent.setScale(0, RoundingMode.HALF_EVEN).toPlainString() + "%";
    }
}
