package com.nest.model;

import com.nest.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.util.Objects;

/** Saves the same amount every month, as long as planned income can cover it. */
public final class FixedAmountSavingsStrategy implements SavingsAllocationStrategy {

    private final BigDecimal amount;

    public FixedAmountSavingsStrategy(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() < 0) {
            throw new BusinessRuleException("Fixed savings amount cannot be negative.");
        }
        this.amount = amount;
    }

    /**
     * @throws BusinessRuleException if the amount exceeds income or has too many decimals for its currency
     */
    @Override
    public Money savingsFor(Money income) {
        Money savings = Money.of(amount, income.currency());
        if (savings.amount().compareTo(income.amount()) > 0) {
            throw new BusinessRuleException("Fixed savings of %s cannot exceed planned income of %s."
                    .formatted(savings, income));
        }
        return savings;
    }
}
