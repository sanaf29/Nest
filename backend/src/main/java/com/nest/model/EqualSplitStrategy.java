package com.nest.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/** Everyone pays the same; leftover cents go to the first participants. */
public final class EqualSplitStrategy implements SplitStrategy {

    @Override
    public List<Money> split(Money total, List<BigDecimal> values) {
        return LargestRemainder.apportion(total, Collections.nCopies(values.size(), BigDecimal.ONE));
    }
}
