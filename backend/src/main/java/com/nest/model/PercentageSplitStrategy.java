package com.nest.model;

import com.nest.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.util.List;

/** Each participant pays a percentage; percentages must total exactly 100. Cents are apportioned exactly. */
public final class PercentageSplitStrategy implements SplitStrategy {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Override
    public List<Money> split(Money total, List<BigDecimal> values) {
        for (BigDecimal v : values) {
            if (v == null || v.signum() < 0 || v.stripTrailingZeros().scale() > 2) {
                throw new BusinessRuleException("Each percentage must be zero or more, with at most 2 decimals.");
            }
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(HUNDRED) != 0) {
            throw new BusinessRuleException("Percentages add up to %s%%, not 100%%.".formatted(sum.stripTrailingZeros().toPlainString()));
        }
        return LargestRemainder.apportion(total, values);
    }
}
