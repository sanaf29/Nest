package com.nest.model;

import com.nest.exception.BusinessRuleException;
import com.nest.exception.ForbiddenOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryTest {

    private User owner;

    @BeforeEach
    void setUp() {
        owner = new User("Sanae", "Faghfouri", "sanae@example.com", "$2a$hash", "CAD");
        ReflectionTestUtils.setField(owner, "id", 1L);
    }

    private Category pets() {
        return new Category(owner, "Pets", CategoryType.EXPENSE, "#f97316");
    }

    @Nested
    class Creation {

        @Test
        void userCategoryIsActiveNotDefaultWithUppercaseColour() {
            Category category = new Category(owner, "  Pets ", CategoryType.EXPENSE, "#f97316");

            assertThat(category.getName()).isEqualTo("Pets");
            assertThat(category.getColor()).isEqualTo("#F97316");
            assertThat(category.isDefault()).isFalse();
            assertThat(category.isArchived()).isFalse();
        }

        @Test
        void missingColourFallsBackToDefault() {
            assertThat(new Category(owner, "Pets", CategoryType.EXPENSE, null).getColor())
                    .isEqualTo(Category.DEFAULT_COLOR);
        }

        @ParameterizedTest
        @ValueSource(strings = {"red", "#FFF", "#GGGGGG", "F97316", "#F973160"})
        void rejectsInvalidColours(String color) {
            assertThatThrownBy(() -> new Category(owner, "Pets", CategoryType.EXPENSE, color))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void rejectsBlankOrOverlongName() {
            assertThatThrownBy(() -> new Category(owner, " ", CategoryType.EXPENSE, null))
                    .isInstanceOf(BusinessRuleException.class);
            assertThatThrownBy(() -> new Category(owner, "x".repeat(51), CategoryType.EXPENSE, null))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Nested
    class VisibilityAndOwnership {

        @Test
        void userCategoryIsVisibleOnlyToItsOwner() {
            Category category = pets();

            assertThat(category.isVisibleTo(1L)).isTrue();
            assertThat(category.isVisibleTo(2L)).isFalse();
            assertThat(category.isOwnedBy(1L)).isTrue();
        }

        @Test
        void defaultCategoryIsVisibleToEveryoneAndOwnedByNoOne() {
            Category salary = CategoryFixtures.defaultCategory(1L, "Salary", CategoryType.INCOME);

            assertThat(salary.isDefault()).isTrue();
            assertThat(salary.isVisibleTo(1L)).isTrue();
            assertThat(salary.isVisibleTo(99L)).isTrue();
            assertThat(salary.isOwnedBy(1L)).isFalse();
        }
    }

    @Nested
    class Changes {

        @Test
        void ownerCanRenameAndRecolour() {
            Category category = pets();

            category.update("Pet Care", "#000000");

            assertThat(category.getName()).isEqualTo("Pet Care");
            assertThat(category.getColor()).isEqualTo("#000000");
        }

        @Test
        void defaultCategoryCannotBeEditedOrArchived() {
            Category groceries = CategoryFixtures.defaultCategory(9L, "Groceries", CategoryType.EXPENSE);

            assertThatThrownBy(() -> groceries.update("Food", null))
                    .isInstanceOf(ForbiddenOperationException.class);
            assertThatThrownBy(groceries::archive)
                    .isInstanceOf(ForbiddenOperationException.class);
            assertThat(groceries.getName()).isEqualTo("Groceries");
            assertThat(groceries.isArchived()).isFalse();
        }

        @Test
        void archiveIsIdempotentAndFreezesTheCategory() {
            Category category = pets();
            category.archive();
            category.archive();

            assertThat(category.isArchived()).isTrue();
            assertThatThrownBy(() -> category.update("Other", null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("archived");
        }
    }

    @Nested
    class UsageInTransactions {

        @Test
        void supportsOnlyTheMatchingTransactionType() {
            Category expense = pets();
            Category income = new Category(owner, "Tutoring", CategoryType.INCOME, null);

            assertThat(expense.supports(TransactionType.EXPENSE)).isTrue();
            assertThat(expense.supports(TransactionType.INCOME)).isFalse();
            assertThat(income.supports(TransactionType.INCOME)).isTrue();
            assertThat(expense.supports(TransactionType.TRANSFER_OUT)).isFalse();
            assertThat(income.supports(TransactionType.TRANSFER_IN)).isFalse();
        }

        @Test
        void requireUsableForRejectsWrongTypeAndArchivedCategories() {
            Category category = pets();

            assertThatCode(() -> category.requireUsableFor(TransactionType.EXPENSE)).doesNotThrowAnyException();
            assertThatThrownBy(() -> category.requireUsableFor(TransactionType.INCOME))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("EXPENSE category");

            category.archive();
            assertThatThrownBy(() -> category.requireUsableFor(TransactionType.EXPENSE))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("archived");
        }
    }
}
