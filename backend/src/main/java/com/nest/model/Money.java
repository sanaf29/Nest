package com.nest.model;

import com.nest.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * An amount of a specific currency. Immutable value object.
 *
 * <p>Rules:
 * <ul>
 *   <li>The scale may not exceed the currency's minor units: 10.555 CAD is rejected, never silently rounded.</li>
 *   <li>Amounts are normalised to exactly the currency's minor units, so 10 CAD and 10.00 CAD are equal.</li>
 *   <li>Arithmetic is only allowed between equal currencies. Nest has no exchange rates.</li>
 * </ul>
 * Derived values (Sprint 2 percentages) will round with {@link #ROUNDING}.
 */
public record Money(BigDecimal amount, String currency) {

    /** Banker's rounding: unbiased when many values are rounded and summed. */
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    public Money {
        Objects.requireNonNull(amount, "amount");
        Currencies.requireSupported(currency);
        int minorUnits = Currencies.minorUnits(currency);
        BigDecimal stripped = amount.stripTrailingZeros();
        if (stripped.scale() > minorUnits) {
            throw new BusinessRuleException("%s allows at most %d decimal place%s, but %s was given."
                    .formatted(currency, minorUnits, minorUnits == 1 ? "" : "s", amount.toPlainString()));
        }
        // Exact: the check above guarantees no non-zero digits are dropped.
        amount = amount.setScale(minorUnits, RoundingMode.UNNECESSARY);
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new BusinessRuleException("Cannot combine %s with %s amounts.".formatted(currency, other.currency));
        }
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency;
    }
}
