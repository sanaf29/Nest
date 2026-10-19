package com.nest.model;

import com.nest.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.util.Objects;

/** Saves a fixed share of income, e.g. 20 % of 3000.00 = 600.00. */
public final class PercentageSavingsStrategy implements SavingsAllocationStrategy {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final BigDecimal percent;

    public PercentageSavingsStrategy(BigDecimal percent) {
        Objects.requireNonNull(percent, "percent");
        if (percent.signum() < 0 || percent.compareTo(HUNDRED) > 0) {
            throw new BusinessRuleException("Savings percentage must be between 0 and 100.");
        }
        if (percent.stripTrailingZeros().scale() > 2) {
            throw new BusinessRuleException("Savings percentage allows at most 2 decimal places.");
        }
        this.percent = percent;
    }

    @Override
    public Money savingsFor(Money income) {
        int minorUnits = Currencies.minorUnits(income.currency());
        BigDecimal savings = income.amount().multiply(percent)
                .divide(HUNDRED, minorUnits, Money.ROUNDING);
        return Money.of(savings, income.currency());
    }

    public BigDecimal percent() {
        return percent;
    }
}
