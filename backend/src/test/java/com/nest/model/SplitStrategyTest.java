package com.nest.model;

import com.nest.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SplitStrategyTest {

    private static Money money(String amount, String currency) {
        return Money.of(new BigDecimal(amount), currency);
    }

    private static List<BigDecimal> values(String... v) {
        return java.util.Arrays.stream(v).map(BigDecimal::new).toList();
    }

    private static List<String> amounts(List<Money> shares) {
        return shares.stream().map(m -> m.amount().toPlainString()).toList();
    }

    @Test
    void equalSplitGivesLeftoverCentsToTheFirstPeople() {
        List<Money> shares = new EqualSplitStrategy().split(money("10.00", "CAD"), Collections.nCopies(3, null));

        assertThat(amounts(shares)).containsExactly("3.34", "3.33", "3.33");
    }

    @ParameterizedTest
    @CsvSource({"0.01,3", "100.00,7", "1999.99,6", "5,20"})
    void equalSplitAlwaysSumsExactly(String total, int people) {
        List<Money> shares = new EqualSplitStrategy().split(money(total, "CAD"), Collections.nCopies(people, null));

        assertThat(shares.stream().reduce(Money.zero("CAD"), Money::plus)).isEqualTo(money(total, "CAD"));
        BigDecimal max = shares.stream().map(Money::amount).max(BigDecimal::compareTo).orElseThrow();
        BigDecimal min = shares.stream().map(Money::amount).min(BigDecimal::compareTo).orElseThrow();
        assertThat(max.subtract(min)).isLessThanOrEqualTo(new BigDecimal("0.01"));
    }

    @Test
    void zeroDecimalCurrencies() {
        assertThat(amounts(new EqualSplitStrategy().split(money("1000", "JPY"), Collections.nCopies(3, null))))
                .containsExactly("334", "333", "333");
    }

    @Test
    void percentageSplitApportionsByLargestRemainder() {
        // 33.33 % / 33.33 % / 33.34 % of 100.00 → exact parts 33.33 / 33.33 / 33.34.
        assertThat(amounts(new PercentageSplitStrategy().split(money("100.00", "CAD"), values("33.33", "33.33", "33.34"))))
                .containsExactly("33.33", "33.33", "33.34");
        // 50/25/25 of 0.03: exact 0.015/0.0075/0.0075 → floors 1,0,0 cent, 2 cents left to the largest remainders.
        List<Money> shares = new PercentageSplitStrategy().split(money("0.03", "CAD"), values("50", "25", "25"));
        assertThat(shares.stream().reduce(Money.zero("CAD"), Money::plus)).isEqualTo(money("0.03", "CAD"));
    }

    @Test
    void percentagesMustTotal100() {
        assertThatThrownBy(() -> new PercentageSplitStrategy().split(money("10", "CAD"), values("50", "49.99")))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("99.99%");
        assertThatThrownBy(() -> new PercentageSplitStrategy().split(money("10", "CAD"), values("150", "-50")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void customAmountsMustAddUpExactly() {
        assertThat(amounts(new CustomAmountSplitStrategy().split(money("90", "CAD"), values("60", "30", "0"))))
                .containsExactly("60.00", "30.00", "0.00");
        assertThatThrownBy(() -> new CustomAmountSplitStrategy().split(money("90", "CAD"), values("60", "29.99")))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("89.99 CAD");
        assertThatThrownBy(() -> new CustomAmountSplitStrategy().split(money("90", "CAD"), values("90.001", "0")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void methodIsAFactory() {
        assertThat(SplitMethod.EQUAL.strategy()).isInstanceOf(EqualSplitStrategy.class);
        assertThat(SplitMethod.CUSTOM.strategy()).isInstanceOf(CustomAmountSplitStrategy.class);
        assertThat(SplitMethod.PERCENTAGE.strategy()).isInstanceOf(PercentageSplitStrategy.class);
    }
}
