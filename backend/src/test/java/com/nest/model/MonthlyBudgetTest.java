package com.nest.model;

import com.nest.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MonthlyBudgetTest {

    private static final YearMonth OCTOBER = YearMonth.of(2026, 10);

    private User owner;
    private Category groceries;
    private Category dining;
    private Category salary;

    @BeforeEach
    void setUp() {
        owner = new User("Sanae", "Faghfouri", "sanae@example.com", "$2a$hash", "CAD");
        ReflectionTestUtils.setField(owner, "id", 1L);
        groceries = CategoryFixtures.defaultCategory(9L, "Groceries", CategoryType.EXPENSE);
        dining = CategoryFixtures.defaultCategory(10L, "Dining Out", CategoryType.EXPENSE);
        salary = CategoryFixtures.defaultCategory(1L, "Salary", CategoryType.INCOME);
    }

    /** Income 3000, 20 % savings → 600 savings, 2400 spendable. */
    private MonthlyBudget budget() {
        return new MonthlyBudget(owner, OCTOBER, "CAD", new BigDecimal("3000"), SavingsStrategyType.PERCENTAGE, BigDecimal.valueOf(20));
    }

    private static MonthlyBudget.AllocationSpec spec(Category category, String limit) {
        return new MonthlyBudget.AllocationSpec(category, new BigDecimal(limit));
    }

    private static Money cad(String amount) {
        return Money.of(new BigDecimal(amount), "CAD");
    }

    @Nested
    class Plan {

        @Test
        void derivesSavingsTargetAndSpendable() {
            MonthlyBudget budget = budget();

            assertThat(budget.savingsTarget()).isEqualTo(cad("600"));
            assertThat(budget.spendable()).isEqualTo(cad("2400"));
            assertThat(budget.period()).isEqualTo(OCTOBER);
        }

        @Test
        void fixedSavingsAboveIncomeIsRejected() {
            assertThatThrownBy(() -> new MonthlyBudget(owner, OCTOBER, "CAD", new BigDecimal("1000"),
                    SavingsStrategyType.FIXED_AMOUNT, new BigDecimal("1200")))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void rejectsNegativeIncomeAndUnsupportedCurrency() {
            assertThatThrownBy(() -> new MonthlyBudget(owner, OCTOBER, "CAD", new BigDecimal("-1"),
                    SavingsStrategyType.PERCENTAGE, BigDecimal.ZERO)).isInstanceOf(BusinessRuleException.class);
            assertThatThrownBy(() -> new MonthlyBudget(owner, OCTOBER, "XAU", BigDecimal.ONE,
                    SavingsStrategyType.PERCENTAGE, BigDecimal.ZERO)).isInstanceOf(BusinessRuleException.class);
        }
    }

    @Nested
    class Allocations {

        @Test
        void acceptsLimitsUpToTheSpendableAmount() {
            MonthlyBudget budget = budget();

            budget.replaceAllocations(List.of(spec(groceries, "2000"), spec(dining, "400")));

            assertThat(budget.allocatedTotal()).isEqualTo(cad("2400"));
            assertThat(budget.getAllocations()).hasSize(2);
        }

        @Test
        void rejectsLimitsAboveSpendable() {
            MonthlyBudget budget = budget();

            assertThatThrownBy(() -> budget.replaceAllocations(List.of(spec(groceries, "2000"), spec(dining, "400.01"))))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("only 2400.00 CAD is available");
            assertThat(budget.getAllocations()).isEmpty();
        }

        @Test
        void rejectsIncomeCategoriesDuplicatesAndNonPositiveLimits() {
            MonthlyBudget budget = budget();

            assertThatThrownBy(() -> budget.replaceAllocations(List.of(spec(salary, "100"))))
                    .isInstanceOf(BusinessRuleException.class).hasMessageContaining("expense categories");
            assertThatThrownBy(() -> budget.replaceAllocations(List.of(spec(groceries, "100"), spec(groceries, "50"))))
                    .isInstanceOf(BusinessRuleException.class).hasMessageContaining("more than once");
            assertThatThrownBy(() -> budget.replaceAllocations(List.of(spec(groceries, "0"))))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void rejectsArchivedOrForeignCategories() {
            Category archived = new Category(owner, "Old", CategoryType.EXPENSE, null);
            ReflectionTestUtils.setField(archived, "id", 50L);
            archived.archive();
            User stranger = new User("X", "Y", "x@example.com", "$2a$hash", "CAD");
            ReflectionTestUtils.setField(stranger, "id", 2L);
            Category foreign = new Category(stranger, "Theirs", CategoryType.EXPENSE, null);
            ReflectionTestUtils.setField(foreign, "id", 51L);
            MonthlyBudget budget = budget();

            assertThatThrownBy(() -> budget.replaceAllocations(List.of(spec(archived, "10"))))
                    .isInstanceOf(BusinessRuleException.class).hasMessageContaining("archived");
            assertThatThrownBy(() -> budget.replaceAllocations(List.of(spec(foreign, "10"))))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void reviseValidatesNewPlanAgainstNewAllocationsAndKeepsExistingRows() {
            MonthlyBudget budget = budget();
            budget.replaceAllocations(List.of(spec(groceries, "2000")));
            BudgetAllocation original = budget.getAllocations().getFirst();

            // Lower income: the old 2000 limit would not fit, but the new 1500 limit does.
            budget.revise(new BigDecimal("2000"), SavingsStrategyType.PERCENTAGE, BigDecimal.valueOf(20),
                    List.of(spec(groceries, "1500")));

            assertThat(budget.getAllocations()).containsExactly(original);
            assertThat(original.limit()).isEqualTo(cad("1500"));
            assertThat(budget.spendable()).isEqualTo(cad("1600"));
        }

        @Test
        void rejectedRevisionChangesNothing() {
            MonthlyBudget budget = budget();
            budget.replaceAllocations(List.of(spec(groceries, "2000")));

            assertThatThrownBy(() -> budget.revise(new BigDecimal("1000"), SavingsStrategyType.PERCENTAGE,
                    BigDecimal.ZERO, List.of(spec(groceries, "2000"))))
                    .isInstanceOf(BusinessRuleException.class);
            assertThat(budget.plannedIncome()).isEqualTo(cad("3000"));
            assertThat(budget.allocatedTotal()).isEqualTo(cad("2000"));
        }

        @Test
        void revisePlanAloneCannotStrandExistingAllocations() {
            MonthlyBudget budget = budget();
            budget.replaceAllocations(List.of(spec(groceries, "2000")));

            assertThatThrownBy(() -> budget.revisePlan(new BigDecimal("1999.99"), SavingsStrategyType.PERCENTAGE, BigDecimal.ZERO))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Nested
    class Summary {

        @Test
        void comparesPlanWithActualsPerCategory() {
            MonthlyBudget budget = budget();
            budget.replaceAllocations(List.of(spec(groceries, "500"), spec(dining, "200")));
            MonthlyActuals actuals = new MonthlyActuals(cad("3100"), Map.of(
                    9L, cad("410.50"),   // groceries: 82.10 % → NEAR_LIMIT
                    10L, cad("250"),     // dining: 125 % → EXCEEDED
                    99L, cad("100")));   // unbudgeted category

            BudgetSummary summary = budget.summarize(actuals);

            assertThat(summary.actualIncome()).isEqualTo(cad("3100"));
            assertThat(summary.actualExpenses()).isEqualTo(cad("760.50"));
            assertThat(summary.netCashFlow()).isEqualTo(cad("2339.50"));
            assertThat(summary.remaining()).isEqualTo(cad("1639.50"));           // 2400 − 760.50
            assertThat(summary.percentUsed()).isEqualByComparingTo("31.69");      // 760.50 / 2400
            assertThat(summary.savingsProgressPercent()).isEqualByComparingTo("389.92"); // 2339.50 / 600
            assertThat(summary.unbudgetedSpending()).isEqualTo(cad("100"));
            assertThat(summary.categories()).extracting(BudgetSummary.CategoryLine::status)
                    .containsExactly(BudgetStatus.NEAR_LIMIT, BudgetStatus.EXCEEDED);
            assertThat(summary.categories().get(0).percentUsed()).isEqualByComparingTo("82.10");
            assertThat(summary.categories().get(1).remaining()).isEqualTo(cad("-50"));
        }

        @Test
        void emptyMonthIsAllOnTrack() {
            MonthlyBudget budget = budget();
            budget.replaceAllocations(List.of(spec(groceries, "500")));

            BudgetSummary summary = budget.summarize(MonthlyActuals.none("CAD"));

            assertThat(summary.remaining()).isEqualTo(cad("2400"));
            assertThat(summary.percentUsed()).isEqualByComparingTo("0");
            assertThat(summary.savingsProgressPercent()).isEqualByComparingTo("0");
            assertThat(summary.categories().getFirst().status()).isEqualTo(BudgetStatus.ON_TRACK);
        }

        @Test
        void zeroIncomeAndZeroSavingsHaveNoPercentages() {
            MonthlyBudget budget = new MonthlyBudget(owner, OCTOBER, "CAD", BigDecimal.ZERO, SavingsStrategyType.PERCENTAGE, BigDecimal.ZERO);

            BudgetSummary summary = budget.summarize(new MonthlyActuals(cad("0"), Map.of(9L, cad("40"))));

            assertThat(summary.percentUsed()).isNull();
            assertThat(summary.savingsProgressPercent()).isNull();
            assertThat(summary.remaining()).isEqualTo(cad("-40"));
            assertThat(summary.netCashFlow()).isEqualTo(cad("-40"));
        }

        @Test
        void overspendingMakesSavingsProgressNegative() {
            MonthlyBudget budget = budget();

            BudgetSummary summary = budget.summarize(new MonthlyActuals(cad("1000"), Map.of(9L, cad("1300"))));

            assertThat(summary.savingsProgressPercent()).isEqualByComparingTo("-50.00");
        }

        @Test
        void refusesActualsInAnotherCurrency() {
            assertThatThrownBy(() -> budget().summarize(MonthlyActuals.none("USD")))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
