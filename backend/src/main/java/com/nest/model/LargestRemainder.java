package com.nest.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Largest-remainder apportionment in the currency's minor units. Each share gets the floor of
 * its exact portion; the leftover cents go, one each, to the largest fractional remainders, with
 * ties broken by participant order. The result always sums exactly to the total:
 * 10.00 ÷ 3 = 3.34 + 3.33 + 3.33.
 */
final class LargestRemainder {

    private LargestRemainder() {
    }

    /** @param weights non-negative, not all zero; shares are proportional to them */
    static List<Money> apportion(Money total, List<BigDecimal> weights) {
        int units = Currencies.minorUnits(total.currency());
        BigInteger totalMinor = total.amount().movePointRight(units).toBigIntegerExact();
        BigDecimal weightSum = weights.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        List<BigInteger> floors = new ArrayList<>();
        List<BigDecimal> remainders = new ArrayList<>();
        for (BigDecimal weight : weights) {
            BigDecimal exact = new BigDecimal(totalMinor).multiply(weight).divide(weightSum, 12, RoundingMode.DOWN);
            BigInteger floor = exact.setScale(0, RoundingMode.FLOOR).toBigIntegerExact();
            floors.add(floor);
            remainders.add(exact.subtract(new BigDecimal(floor)));
        }
        int leftover = totalMinor.subtract(floors.stream().reduce(BigInteger.ZERO, BigInteger::add)).intValueExact();
        List<Integer> order = IntStream.range(0, weights.size()).boxed()
                .sorted(Comparator.comparing((Integer i) -> remainders.get(i)).reversed().thenComparing(i -> i))
                .toList();
        for (int k = 0; k < leftover; k++) {
            int i = order.get(k % order.size());
            floors.set(i, floors.get(i).add(BigInteger.ONE));
        }
        return floors.stream()
                .map(minor -> Money.of(new BigDecimal(minor).movePointLeft(units), total.currency()))
                .toList();
    }
}
