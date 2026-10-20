package com.nest.model;

import com.nest.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.util.List;

/** Each participant's amount is given; they must add up exactly to the total. */
public final class CustomAmountSplitStrategy implements SplitStrategy {

    @Override
    public List<Money> split(Money total, List<BigDecimal> values) {
        List<Money> shares = values.stream()
                .map(v -> {
                    if (v == null || v.signum() < 0) {
                        throw new BusinessRuleException("Each person needs an amount of zero or more.");
                    }
                    return Money.of(v, total.currency());
                })
                .toList();
        Money sum = shares.stream().reduce(Money.zero(total.currency()), Money::plus);
        if (!sum.equals(total)) {
            throw new BusinessRuleException("The amounts add up to %s, but the expense is %s.".formatted(sum, total));
        }
        return shares;
    }
}
