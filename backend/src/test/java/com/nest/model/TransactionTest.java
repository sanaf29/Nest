package com.nest.model;

import com.nest.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 18);

    private User owner;
    private FinancialAccount chequing;
    private Category groceries;
    private Category salary;

    @BeforeEach
    void setUp() {
        owner = user(1L);
        chequing = account(owner, 10L, "Chequing", "CAD");
        groceries = CategoryFixtures.defaultCategory(100L, "Groceries", CategoryType.EXPENSE);
        salary = CategoryFixtures.defaultCategory(101L, "Salary", CategoryType.INCOME);
    }

    private static User user(long id) {
        User user = new User("Test", "User", "u" + id + "@example.com", "$2a$hash", "CAD");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static FinancialAccount account(User owner, long id, String name, String currency) {
        FinancialAccount account = new FinancialAccount(owner, name, AccountType.CHECKING, currency, BigDecimal.ZERO);
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }

    private Transaction expense(String amount) {
        return new Transaction(chequing, groceries, TransactionType.EXPENSE, new BigDecimal(amount), "Food", DATE);
    }

    @Nested
    class Creation {

        @Test
        void recordsExpenseWithNormalisedAmountAndAccountCurrency() {
            Transaction transaction = expense("84.3");

            assertThat(transaction.money()).isEqualTo(Money.of(new BigDecimal("84.30"), "CAD"));
            assertThat(transaction.currency()).isEqualTo("CAD");
            assertThat(transaction.signedAmount()).isEqualByComparingTo("-84.30");
        }

        @Test
        void incomeHasPositiveEffect() {
            Transaction income = new Transaction(chequing, salary, TransactionType.INCOME, new BigDecimal("3000"), null, DATE);

            assertThat(income.signedAmount()).isEqualByComparingTo("3000.00");
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "-5.00"})
        void rejectsNonPositiveAmounts(String amount) {
            assertThatThrownBy(() -> expense(amount))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("greater than zero");
        }

        @Test
        void rejectsMorePrecisionThanTheAccountCurrencyAllows() {
            FinancialAccount yen = account(owner, 11L, "Yen", "JPY");

            assertThatThrownBy(() -> new Transaction(yen, groceries, TransactionType.EXPENSE, new BigDecimal("10.5"), null, DATE))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("JPY");
        }

        @Test
        void rejectsCategoryOfTheWrongType() {
            assertThatThrownBy(() -> new Transaction(chequing, salary, TransactionType.EXPENSE, BigDecimal.TEN, null, DATE))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("INCOME category");
        }

        @Test
        void rejectsArchivedCategory() {
            Category pets = new Category(owner, "Pets", CategoryType.EXPENSE, null);
            pets.archive();

            assertThatThrownBy(() -> new Transaction(chequing, pets, TransactionType.EXPENSE, BigDecimal.TEN, null, DATE))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("archived");
        }

        @Test
        void rejectsAnotherUsersCategory() {
            Category strangers = new Category(user(2L), "Hobby", CategoryType.EXPENSE, null);

            assertThatThrownBy(() -> new Transaction(chequing, strangers, TransactionType.EXPENSE, BigDecimal.TEN, null, DATE))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void requiresACategory() {
            assertThatThrownBy(() -> new Transaction(chequing, null, TransactionType.EXPENSE, BigDecimal.TEN, null, DATE))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @ParameterizedTest
        @EnumSource(value = TransactionType.class, names = {"TRANSFER_IN", "TRANSFER_OUT"})
        void transferTypesCannotBeRecordedDirectly(TransactionType type) {
            assertThatThrownBy(() -> new Transaction(chequing, groceries, type, BigDecimal.TEN, null, DATE))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("/api/transfers");
        }

        @Test
        void rejectsClosedAccount() {
            chequing.deactivate();

            assertThatThrownBy(() -> expense("10"))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("closed");
        }

        @Test
        void blankDescriptionBecomesNullAndTextIsTrimmed() {
            assertThat(new Transaction(chequing, groceries, TransactionType.EXPENSE, BigDecimal.ONE, "  ", DATE).getDescription()).isNull();
            assertThat(new Transaction(chequing, groceries, TransactionType.EXPENSE, BigDecimal.ONE, " Milk ", DATE).getDescription()).isEqualTo("Milk");
            assertThatThrownBy(() -> new Transaction(chequing, groceries, TransactionType.EXPENSE, BigDecimal.ONE, "x".repeat(256), DATE))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Nested
    class Revision {

        @Test
        void replacesAllFieldsAndRechecksRules() {
            Transaction transaction = expense("10");
            FinancialAccount savings = account(owner, 12L, "Savings", "CAD");

            transaction.revise(savings, salary, TransactionType.INCOME, new BigDecimal("99.99"), "Refund", DATE.plusDays(1));

            assertThat(transaction.getAccount()).isSameAs(savings);
            assertThat(transaction.getType()).isEqualTo(TransactionType.INCOME);
            assertThat(transaction.signedAmount()).isEqualByComparingTo("99.99");
            assertThat(transaction.getTransactionDate()).isEqualTo(DATE.plusDays(1));
        }

        @Test
        void invalidRevisionLeavesTheTransactionUnchanged() {
            Transaction transaction = expense("10");

            assertThatThrownBy(() -> transaction.revise(chequing, salary, TransactionType.EXPENSE, BigDecimal.ONE, null, DATE))
                    .isInstanceOf(BusinessRuleException.class);
            assertThat(transaction.getCategory()).isSameAs(groceries);
            assertThat(transaction.money().amount()).isEqualByComparingTo("10.00");
        }

        @Test
        void cannotMoveToAnotherUsersAccount() {
            Transaction transaction = expense("10");
            FinancialAccount foreign = account(user(2L), 20L, "Foreign", "CAD");

            assertThatThrownBy(() -> transaction.revise(foreign, groceries, TransactionType.EXPENSE, BigDecimal.ONE, null, DATE))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void entriesOnAClosedAccountAreFrozen() {
            Transaction transaction = expense("10");
            chequing.deactivate();

            assertThatThrownBy(() -> transaction.revise(chequing, groceries, TransactionType.EXPENSE, BigDecimal.ONE, null, DATE))
                    .isInstanceOf(BusinessRuleException.class);
            assertThatThrownBy(transaction::requireRemovable).isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void entriesOnAnOpenAccountAreRemovable() {
            assertThatCode(() -> expense("10").requireRemovable()).doesNotThrowAnyException();
        }
    }
}
