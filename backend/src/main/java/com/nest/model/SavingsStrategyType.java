package com.nest.model;

import java.math.BigDecimal;

/**
 * The stored kind of savings strategy. Each constant is also a factory for its strategy
 * (factory method), so the budget stores a type and a value and rebuilds the behaviour on demand.
 */
public enum SavingsStrategyType {

    /** {@code value} is a percentage of planned income, 0–100. */
    PERCENTAGE {
        @Override
        public SavingsAllocationStrategy create(BigDecimal value) {
            return new PercentageSavingsStrategy(value);
        }
    },

    /** {@code value} is an amount in the budget's currency. */
    FIXED_AMOUNT {
        @Override
        public SavingsAllocationStrategy create(BigDecimal value) {
            return new FixedAmountSavingsStrategy(value);
        }
    };

    public abstract SavingsAllocationStrategy create(BigDecimal value);
}
