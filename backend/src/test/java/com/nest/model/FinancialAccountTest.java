package com.nest.model;

import com.nest.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinancialAccountTest {

    private User owner;

    @BeforeEach
    void setUp() {
        owner = new User("Sanae", "Faghfouri", "sanae@example.com", "$2a$hash", "CAD");
        ReflectionTestUtils.setField(owner, "id", 1L);
    }

    private FinancialAccount account(AccountType type, String opening) {
        return new FinancialAccount(owner, "Main", type, "CAD", new BigDecimal(opening));
    }

    @Nested
    class Creation {

        @Test
        void startsActiveWithTrimmedNameAndNormalisedOpeningBalance() {
            FinancialAccount account = new FinancialAccount(owner, "  Everyday  ", AccountType.CHECKING, "CAD",
                    new BigDecimal("1500"));

            assertThat(account.isActive()).isTrue();
            assertThat(account.getName()).isEqualTo("Everyday");
            assertThat(account.openingBalance()).isEqualTo(Money.of(new BigDecimal("1500.00"), "CAD"));
        }

        @ParameterizedTest
        @EnumSource(value = AccountType.class, names = {"CHECKING", "CREDIT_CARD"})
        void checkingAndCreditCardMayStartNegative(AccountType type) {
            assertThat(account(type, "-250.00").openingBalance().isNegative()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = AccountType.class, names = {"SAVINGS", "CASH"})
        void savingsAndCashMayNotStartNegative(AccountType type) {
            assertThatThrownBy(() -> account(type, "-0.01"))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("negative opening balance");
        }

        @Test
        void rejectsOpeningBalanceWithMorePrecisionThanTheCurrency() {
            assertThatThrownBy(() -> new FinancialAccount(owner, "Yen", AccountType.CASH, "JPY", new BigDecimal("10.5")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("JPY");
        }

        @Test
        void rejectsBlankOrOverlongNames() {
            assertThatThrownBy(() -> new FinancialAccount(owner, " ", AccountType.CASH, "CAD", BigDecimal.ZERO))
                    .isInstanceOf(BusinessRuleException.class);
            assertThatThrownBy(() -> new FinancialAccount(owner, "x".repeat(101), AccountType.CASH, "CAD", BigDecimal.ZERO))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void rejectsUnsupportedCurrency() {
            assertThatThrownBy(() -> new FinancialAccount(owner, "Gold", AccountType.CASH, "XAU", BigDecimal.ZERO))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Nested
    class Changes {

        @Test
        void renameAndCorrectOpeningBalance() {
            FinancialAccount account = account(AccountType.SAVINGS, "100");

            account.rename("Emergency Fund");
            account.correctOpeningBalance(new BigDecimal("250.50"));

            assertThat(account.getName()).isEqualTo("Emergency Fund");
            assertThat(account.openingBalance().amount()).isEqualByComparingTo("250.50");
        }

        @Test
        void correctionStillEnforcesTheTypeRule() {
            FinancialAccount account = account(AccountType.SAVINGS, "100");

            assertThatThrownBy(() -> account.correctOpeningBalance(new BigDecimal("-1")))
                    .isInstanceOf(BusinessRuleException.class);
            assertThat(account.openingBalance().amount()).isEqualByComparingTo("100");
        }

        @Test
        void closedAccountCannotBeChangedButDeactivateIsIdempotent() {
            FinancialAccount account = account(AccountType.CHECKING, "0");
            account.deactivate();
            account.deactivate();

            assertThat(account.isActive()).isFalse();
            assertThatThrownBy(() -> account.rename("New")).isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("closed");
            assertThatThrownBy(() -> account.correctOpeningBalance(BigDecimal.TEN))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Nested
    class Ownership {

        @Test
        void isOwnedOnlyByItsUser() {
            FinancialAccount account = account(AccountType.CASH, "0");

            assertThat(account.isOwnedBy(1L)).isTrue();
            assertThat(account.isOwnedBy(2L)).isFalse();
        }
    }

    @Nested
    class Balance {

        @Test
        void withoutActivityEqualsOpeningBalance() {
            assertThat(account(AccountType.CHECKING, "1500").calculateBalance(Map.of()))
                    .isEqualTo(Money.of(new BigDecimal("1500"), "CAD"));
        }

        @Test
        void addsInflowsAndSubtractsOutflows() {
            Map<TransactionType, BigDecimal> totals = new EnumMap<>(TransactionType.class);
            totals.put(TransactionType.INCOME, new BigDecimal("3000.00"));
            totals.put(TransactionType.EXPENSE, new BigDecimal("1245.67"));
            totals.put(TransactionType.TRANSFER_IN, new BigDecimal("100.00"));
            totals.put(TransactionType.TRANSFER_OUT, new BigDecimal("600.00"));

            Money balance = account(AccountType.CHECKING, "500.00").calculateBalance(totals);

            // 500 + 3000 + 100 - 1245.67 - 600
            assertThat(balance).isEqualTo(Money.of(new BigDecimal("1754.33"), "CAD"));
        }

        @Test
        void creditCardConventionPurchasesIncreaseDebtAndPaymentsReduceIt() {
            Map<TransactionType, BigDecimal> totals = new EnumMap<>(TransactionType.class);
            totals.put(TransactionType.EXPENSE, new BigDecimal("320.00"));     // purchases
            totals.put(TransactionType.TRANSFER_IN, new BigDecimal("200.00")); // payment from chequing

            Money balance = account(AccountType.CREDIT_CARD, "-100.00").calculateBalance(totals);

            assertThat(balance).isEqualTo(Money.of(new BigDecimal("-220.00"), "CAD")); // $220 owed
            assertThat(balance.isNegative()).isTrue();
        }

        @Test
        void totalsReadFromTheDatabaseWithFourDecimalScaleAreAccepted() {
            Map<TransactionType, BigDecimal> totals = Map.of(TransactionType.INCOME, new BigDecimal("10.5000"));

            assertThat(account(AccountType.CASH, "0").calculateBalance(totals).amount())
                    .isEqualTo(new BigDecimal("10.50"));
        }
    }
}
