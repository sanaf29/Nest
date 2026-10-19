package com.nest.model;

import com.nest.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrenciesTest {

    @ParameterizedTest
    @ValueSource(strings = {"CAD", "USD", "EUR", "JPY", "MAD", "BHD"})
    void acceptsRealIsoCurrencies(String code) {
        assertThat(Currencies.isSupported(code)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"cad", "Cad", "CADX", "ZZZ", "XAU", "XXX", " CAD"})
    void rejectsMalformedUnknownOrPseudoCurrencies(String code) {
        assertThat(Currencies.isSupported(code)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"CAD,2", "JPY,0", "BHD,3"})
    void reportsMinorUnits(String code, int expected) {
        assertThat(Currencies.minorUnits(code)).isEqualTo(expected);
    }

    @Test
    void requireSupportedThrowsDomainException() {
        assertThatThrownBy(() -> Currencies.requireSupported("ABC"))
                .isInstanceOf(BusinessRuleException.class);
    }
}
