package com.nest.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Divides a total among participants (Strategy pattern). Every implementation returns shares in
 * the total's currency that add up <em>exactly</em> to the total.
 */
public interface SplitStrategy {

    /**
     * @param total  the expense
     * @param values one per participant, in order; meaning depends on the strategy (ignored, an amount, or a percentage)
     */
    List<Money> split(Money total, List<BigDecimal> values);
}
