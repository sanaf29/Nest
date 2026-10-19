package com.nest.model;

import com.nest.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    private static Money cad(String amount) {
        return Money.of(new BigDecimal(amount), "CAD");
    }

    @ParameterizedTest
    @CsvSource({"10,10.00", "10.5,10.50", "10.50,10.50", "10.0000,10.00", "-3.1,-3.10", "0,0.00"})
    void normalisesToTheCurrencysMinorUnits(String input, String expected) {
        assertThat(cad(input).amount()).isEqualTo(new BigDecimal(expected));
    }

    @Test
    void equalAmountsWithDifferentScalesAreEqual() {
        assertThat(cad("10")).isEqualTo(cad("10.00"));
    }

    @ParameterizedTest
    @CsvSource({"10.555,CAD", "0.001,USD", "10.5,JPY", "1.0001,BHD"})
    void rejectsMorePrecisionThanTheCurrencyAllowsInsteadOfRounding(String amount, String currency) {
        assertThatThrownBy(() -> Money.of(new BigDecimal(amount), currency))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(currency);
    }

    @Test
    void zeroDecimalAndThreeDecimalCurrenciesAreSupported() {
        assertThat(Money.of(new BigDecimal("1500"), "JPY").amount()).isEqualTo(new BigDecimal("1500"));
        assertThat(Money.of(new BigDecimal("1.234"), "BHD").amount()).isEqualTo(new BigDecimal("1.234"));
    }

    @Test
    void addsAndSubtractsExactly() {
        // The classic floating-point trap: 0.1 + 0.2 must be exactly 0.30.
        assertThat(cad("0.10").plus(cad("0.20"))).isEqualTo(cad("0.30"));
        assertThat(cad("100.00").minus(cad("150.25"))).isEqualTo(cad("-50.25"));
    }

    @Test
    void refusesToMixCurrencies() {
        Money usd = Money.of(BigDecimal.ONE, "USD");

        assertThatThrownBy(() -> cad("1").plus(usd))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CAD")
                .hasMessageContaining("USD");
    }

    @Test
    void rejectsUnsupportedCurrency() {
        assertThatThrownBy(() -> Money.of(BigDecimal.ONE, "XAU")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void reportsSign() {
        assertThat(cad("-0.01").isNegative()).isTrue();
        assertThat(cad("0").isNegative()).isFalse();
        assertThat(cad("0").isPositive()).isFalse();
        assertThat(cad("0.01").isPositive()).isTrue();
    }
}
