package com.nest.model;

import com.nest.exception.BusinessRuleException;
import com.nest.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransferTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 19);

    private User owner;
    private FinancialAccount chequing;
    private FinancialAccount savings;

    @BeforeEach
    void setUp() {
        owner = user(1L);
        chequing = account(owner, 10L, "Chequing", AccountType.CHECKING, "CAD");
        savings = account(owner, 11L, "Savings", AccountType.SAVINGS, "CAD");
    }

    private static User user(long id) {
        User user = new User("Test", "User", "u" + id + "@example.com", "$2a$hash", "CAD");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static FinancialAccount account(User owner, long id, String name, AccountType type, String currency) {
        FinancialAccount account = new FinancialAccount(owner, name, type, currency, BigDecimal.ZERO);
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }

    @Nested
    class Creation {

        @Test
        void createsOneOutgoingAndOneIncomingEntryWithEqualAmountsAndDate() {
            Transfer transfer = Transfer.between(chequing, savings, new BigDecimal("500"), DATE, "Monthly savings");

            assertThat(transfer.legs()).hasSize(2);
            assertThat(transfer.outgoing().getType()).isEqualTo(TransactionType.TRANSFER_OUT);
            assertThat(transfer.outgoing().getAccount()).isSameAs(chequing);
            assertThat(transfer.incoming().getType()).isEqualTo(TransactionType.TRANSFER_IN);
            assertThat(transfer.incoming().getAccount()).isSameAs(savings);
            assertThat(transfer.outgoing().money()).isEqualTo(transfer.incoming().money());
            assertThat(transfer.amount()).isEqualTo(Money.of(new BigDecimal("500.00"), "CAD"));
            assertThat(transfer.legs()).allSatisfy(leg -> {
                assertThat(leg.getTransactionDate()).isEqualTo(DATE);
                assertThat(leg.getCategory()).isNull();
                assertThat(leg.getTransfer()).isSameAs(transfer);
                assertThat(leg.getDescription()).isEqualTo("Monthly savings");
            });
        }

        @Test
        void legsCancelOutSoNetWorthIsUnchanged() {
            Transfer transfer = Transfer.between(chequing, savings, new BigDecimal("123.45"), DATE, null);

            assertThat(transfer.outgoing().signedAmount().add(transfer.incoming().signedAmount()))
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void missingDescriptionDescribesEachSide() {
            Transfer transfer = Transfer.between(chequing, savings, BigDecimal.TEN, DATE, "  ");

            assertThat(transfer.outgoing().getDescription()).isEqualTo("Transfer to Savings");
            assertThat(transfer.incoming().getDescription()).isEqualTo("Transfer from Chequing");
        }

        @Test
        void creditCardPaymentReducesTheAmountOwed() {
            FinancialAccount visa = new FinancialAccount(owner, "Visa", AccountType.CREDIT_CARD, "CAD", new BigDecimal("-300"));
            ReflectionTestUtils.setField(visa, "id", 12L);

            Transfer payment = Transfer.between(chequing, visa, new BigDecimal("200"), DATE, "Card payment");
            Map<TransactionType, BigDecimal> visaTotals = new EnumMap<>(TransactionType.class);
            visaTotals.put(TransactionType.TRANSFER_IN, payment.incoming().money().amount());

            assertThat(visa.calculateBalance(visaTotals)).isEqualTo(Money.of(new BigDecimal("-100"), "CAD"));
        }
    }

    @Nested
    class Rules {

        @Test
        void accountsMustDiffer() {
            assertThatThrownBy(() -> Transfer.between(chequing, chequing, BigDecimal.TEN, DATE, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("different accounts");
        }

        @Test
        void currenciesMustMatch() {
            FinancialAccount usd = account(owner, 13L, "USD", AccountType.CHECKING, "USD");

            assertThatThrownBy(() -> Transfer.between(chequing, usd, BigDecimal.TEN, DATE, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("currency conversion");
        }

        @Test
        void accountsMustHaveTheSameOwner() {
            FinancialAccount foreign = account(user(2L), 20L, "Foreign", AccountType.CHECKING, "CAD");

            assertThatThrownBy(() -> Transfer.between(chequing, foreign, BigDecimal.TEN, DATE, null))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void bothAccountsMustBeOpen() {
            savings.deactivate();

            assertThatThrownBy(() -> Transfer.between(chequing, savings, BigDecimal.TEN, DATE, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("closed");
            assertThatThrownBy(() -> Transfer.between(savings, chequing, BigDecimal.TEN, DATE, null))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "-1", "10.555"})
        void amountMustBePositiveWithValidPrecision(String amount) {
            assertThatThrownBy(() -> Transfer.between(chequing, savings, new BigDecimal(amount), DATE, null))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Nested
    class EntryProtection {

        @Test
        void entriesCannotBeRevisedOrRemovedIndividually() {
            Transfer transfer = Transfer.between(chequing, savings, BigDecimal.TEN, DATE, null);
            ReflectionTestUtils.setField(transfer, "id", 7L);
            Category groceries = CategoryFixtures.defaultCategory(100L, "Groceries", CategoryType.EXPENSE);

            assertThatThrownBy(() -> transfer.outgoing().revise(chequing, groceries, TransactionType.EXPENSE,
                    BigDecimal.ONE, null, DATE))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("/api/transfers/7");
            assertThatThrownBy(() -> transfer.incoming().requireRemovable())
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        void transferIsRemovableOnlyWhileBothAccountsAreOpen() {
            Transfer transfer = Transfer.between(chequing, savings, BigDecimal.TEN, DATE, null);
            assertThatCode(transfer::requireRemovable).doesNotThrowAnyException();

            savings.deactivate();
            assertThatThrownBy(transfer::requireRemovable).isInstanceOf(BusinessRuleException.class);
        }
    }
}
