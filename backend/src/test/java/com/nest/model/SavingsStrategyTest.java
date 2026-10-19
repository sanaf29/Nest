package com.nest.model;

import com.nest.exception.BusinessRuleException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SavingsStrategyTest {

    private static Money money(String amount, String currency) {
        return Money.of(new BigDecimal(amount), currency);
    }

    @Nested
    class Percentage {

        @ParameterizedTest
        @CsvSource({
                "3000.00, 20, CAD, 600.00",   // the specification's example
                "3000.00, 0, CAD, 0.00",
                "3000.00, 100, CAD, 3000.00",
                "1000.00, 33.33, CAD, 333.30",
                "100.05, 50, CAD, 50.02",     // 50.025 -> HALF_EVEN -> 50.02
                "100.15, 50, CAD, 50.08",     // 50.075 -> HALF_EVEN -> 50.08
                "12345, 10, JPY, 1234",       // 1234.5 -> HALF_EVEN -> 1234
                "0.00, 20, CAD, 0.00"         // zero income
        })
        void savesAShareOfIncomeRoundedHalfEvenToMinorUnits(String income, String percent, String currency, String expected) {
            Money savings = new PercentageSavingsStrategy(new BigDecimal(percent)).savingsFor(money(income, currency));

            assertThat(savings).isEqualTo(money(expected, currency));
        }

        @ParameterizedTest
        @ValueSource(strings = {"-1", "100.01", "10.555"})
        void rejectsOutOfRangeOrOverlyPrecisePercentages(String percent) {
            assertThatThrownBy(() -> new PercentageSavingsStrategy(new BigDecimal(percent)))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Nested
    class FixedAmount {

        @Test
        void savesTheSameAmountRegardlessOfIncome() {
            SavingsAllocationStrategy strategy = new FixedAmountSavingsStrategy(new BigDecimal("500"));

            assertThat(strategy.savingsFor(money("3000", "CAD"))).isEqualTo(money("500.00", "CAD"));
            assertThat(strategy.savingsFor(money("500", "CAD"))).isEqualTo(money("500.00", "CAD"));
        }

        @Test
        void cannotExceedIncome() {
            assertThatThrownBy(() -> new FixedAmountSavingsStrategy(new BigDecimal("500.01")).savingsFor(money("500", "CAD")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("cannot exceed planned income");
        }

        @Test
        void rejectsNegativeAmountsAndExcessPrecision() {
            assertThatThrownBy(() -> new FixedAmountSavingsStrategy(new BigDecimal("-1")))
                    .isInstanceOf(BusinessRuleException.class);
            assertThatThrownBy(() -> new FixedAmountSavingsStrategy(new BigDecimal("10.5")).savingsFor(money("100", "JPY")))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Test
    void typeIsAFactoryForItsStrategy() {
        assertThat(SavingsStrategyType.PERCENTAGE.create(BigDecimal.TEN)).isInstanceOf(PercentageSavingsStrategy.class);
        assertThat(SavingsStrategyType.FIXED_AMOUNT.create(BigDecimal.TEN)).isInstanceOf(FixedAmountSavingsStrategy.class);
    }

    @Test
    void budgetStatusThresholds() {
        assertThat(BudgetStatus.of(new BigDecimal("79.99"))).isEqualTo(BudgetStatus.ON_TRACK);
        assertThat(BudgetStatus.of(new BigDecimal("80.00"))).isEqualTo(BudgetStatus.NEAR_LIMIT);
        assertThat(BudgetStatus.of(new BigDecimal("100.00"))).isEqualTo(BudgetStatus.NEAR_LIMIT);
        assertThat(BudgetStatus.of(new BigDecimal("100.01"))).isEqualTo(BudgetStatus.EXCEEDED);
    }
}
