package com.nest.service.insight;

import com.nest.model.BudgetSummary;
import com.nest.model.Category;
import com.nest.model.CategoryFixtures;
import com.nest.model.CategoryType;
import com.nest.model.Money;
import com.nest.model.MonthlyActuals;
import com.nest.model.MonthlyBudget;
import com.nest.model.SavingsStrategyType;
import com.nest.model.User;
import com.nest.service.insight.rules.BudgetExceededRule;
import com.nest.service.insight.rules.BudgetNearLimitRule;
import com.nest.service.insight.rules.HighSpendingRule;
import com.nest.service.insight.rules.SavingsProgressRule;
import com.nest.service.insight.rules.SavingsTargetMetRule;
import com.nest.service.insight.rules.SpendingTrendRule;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InsightRulesTest {

    private static final YearMonth OCTOBER = YearMonth.of(2026, 10);
    private static final LocalDate AS_OF = LocalDate.of(2026, 10, 19);

    private static Money cad(String amount) {
        return Money.of(new BigDecimal(amount), "CAD");
    }

    private static FinancialSummary summary(String income, String expenses, String toDate, String lastMonthSamePeriod,
                                            Optional<BudgetSummary> budget, List<FinancialSummary.GoalSnapshot> goals) {
        return new FinancialSummary(OCTOBER, "CAD", AS_OF, cad(income), cad(expenses), cad(toDate), cad(lastMonthSamePeriod),
                budget, goals, List.of());
    }

    private static FinancialSummary plain(String income, String expenses) {
        return summary(income, expenses, expenses, "0", Optional.empty(), List.of());
    }

    /** Budget: 3000 income, 20 % savings; Groceries limit 500, Dining limit 200. */
    private static BudgetSummary budget(String groceriesSpent, String diningSpent, String income) {
        User owner = new User("A", "B", "a@example.com", "$2a$hash", "CAD");
        ReflectionTestUtils.setField(owner, "id", 1L);
        Category groceries = CategoryFixtures.defaultCategory(9L, "Groceries", CategoryType.EXPENSE);
        Category dining = CategoryFixtures.defaultCategory(10L, "Dining Out", CategoryType.EXPENSE);
        MonthlyBudget b = new MonthlyBudget(owner, OCTOBER, "CAD", new BigDecimal("3000"), SavingsStrategyType.PERCENTAGE, BigDecimal.valueOf(20));
        b.replaceAllocations(List.of(new MonthlyBudget.AllocationSpec(groceries, new BigDecimal("500")),
                new MonthlyBudget.AllocationSpec(dining, new BigDecimal("200"))));
        return b.summarize(new MonthlyActuals(cad(income), Map.of(9L, cad(groceriesSpent), 10L, cad(diningSpent))));
    }

    @Nested
    class Budget {

        @Test
        void exceededCategoriesAreAnAlertListingEachOne() {
            Optional<FinancialInsight> insight = new BudgetExceededRule()
                    .evaluate(summary("3000", "760", "760", "0", Optional.of(budget("510", "250", "3000")), List.of()));

            assertThat(insight).hasValueSatisfying(i -> {
                assertThat(i.code()).isEqualTo("BUDGET_EXCEEDED");
                assertThat(i.severity()).isEqualTo(InsightSeverity.ALERT);
                assertThat(i.title()).isEqualTo("2 categories are over budget");
                assertThat(i.message()).contains("Dining Out: 250.00 CAD of 200.00 CAD (125%)").contains("Groceries: 510.00 CAD of 500.00 CAD (102%)");
                assertThat(i.message().indexOf("Dining Out")).isLessThan(i.message().indexOf("Groceries")); // worst first
            });
        }

        @Test
        void nearLimitIsAWarningAndExactly100PercentIsNotExceeded() {
            FinancialSummary s = summary("3000", "600", "600", "0", Optional.of(budget("400", "200", "3000")), List.of());

            assertThat(new BudgetExceededRule().evaluate(s)).isEmpty();
            assertThat(new BudgetNearLimitRule().evaluate(s)).hasValueSatisfying(i -> {
                assertThat(i.title()).isEqualTo("2 categories are close to their limits");
                assertThat(i.message()).contains("Groceries has 100.00 CAD left (80% used)");
            });
        }

        @Test
        void noBudgetMeansNoBudgetInsights() {
            assertThat(new BudgetExceededRule().evaluate(plain("1", "1"))).isEmpty();
            assertThat(new BudgetNearLimitRule().evaluate(plain("1", "1"))).isEmpty();
            assertThat(new SavingsTargetMetRule().evaluate(plain("1", "1"))).isEmpty();
        }

        @Test
        void savingsTargetMetIsPositiveFeedback() {
            FinancialSummary s = summary("3000", "100", "100", "0", Optional.of(budget("50", "50", "3000")), List.of());

            assertThat(new SavingsTargetMetRule().evaluate(s)).hasValueSatisfying(i -> {
                assertThat(i.severity()).isEqualTo(InsightSeverity.POSITIVE);
                assertThat(i.message()).contains("2900.00 CAD").contains("600.00 CAD");
            });
        }
    }

    @Nested
    class Spending {

        @Test
        void spendingMoreThanIncomeIsAWarningWithTheGap() {
            assertThat(new HighSpendingRule().evaluate(plain("2000", "2350.50"))).hasValueSatisfying(i ->
                    assertThat(i.message()).isEqualTo("Expenses of 2350.50 CAD are 350.50 CAD more than your income of 2000.00 CAD."));
            assertThat(new HighSpendingRule().evaluate(plain("2000", "2000"))).isEmpty();
            assertThat(new HighSpendingRule().evaluate(plain("0", "50"))).isEmpty(); // no income recorded: nothing to compare
        }

        @Test
        void trendComparesTheSameNumberOfDays() {
            FinancialSummary up = summary("0", "900", "600", "500", Optional.empty(), List.of());
            FinancialSummary flat = summary("0", "900", "599", "500", Optional.empty(), List.of());
            FinancialSummary noHistory = summary("0", "900", "600", "0", Optional.empty(), List.of());

            assertThat(new SpendingTrendRule().evaluate(up)).hasValueSatisfying(i -> {
                assertThat(i.title()).isEqualTo("Spending is up 20% compared with last month");
                assertThat(i.message()).contains("first 19 days").contains("600.00 CAD").contains("500.00 CAD");
            });
            assertThat(new SpendingTrendRule().evaluate(flat)).isEmpty();
            assertThat(new SpendingTrendRule().evaluate(noHistory)).isEmpty();
        }
    }

    @Nested
    class Goals {

        private FinancialSummary.GoalSnapshot goal(String name, String saved, String target, LocalDate start, LocalDate end, boolean achieved) {
            Money s = cad(saved);
            Money t = cad(target);
            BigDecimal pct = s.amount().multiply(BigDecimal.valueOf(100)).divide(t.amount(), 2, java.math.RoundingMode.HALF_EVEN);
            return new FinancialSummary.GoalSnapshot(name, s, t, start, end, pct, achieved);
        }

        @Test
        void expectedProgressIsTheElapsedShareOfTheSchedule() {
            assertThat(SavingsProgressRule.expectedPercent(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), LocalDate.of(2026, 7, 2)))
                    .isEqualByComparingTo("50.00");
            assertThat(SavingsProgressRule.expectedPercent(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 11), LocalDate.of(2025, 12, 1)))
                    .isEqualByComparingTo("0");
            assertThat(SavingsProgressRule.expectedPercent(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 11), LocalDate.of(2027, 1, 1)))
                    .isEqualByComparingTo("100");
        }

        @Test
        void goalMoreThanTenPointsBehindIsFlaggedWithAMonthlyAmount() {
            // Half the time has passed (Apr 19 → Apr 19 next year, as of Oct 19) but only 30 % is saved.
            FinancialSummary s = summary("0", "0", "0", "0", Optional.empty(), List.of(
                    goal("Trip", "300", "1000", LocalDate.of(2026, 4, 19), LocalDate.of(2027, 4, 19), false)));

            assertThat(new SavingsProgressRule().evaluate(s)).hasValueSatisfying(i -> {
                assertThat(i.title()).isEqualTo("'Trip' is behind schedule");
                assertThat(i.message()).contains("30% funded").contains("50% of the time")
                        .contains("about 116.67 CAD a month"); // 700 over 6 months, rounded up
            });
        }

        @Test
        void goalsOnTrackAchievedOrWithoutDeadlineAreNotFlagged() {
            FinancialSummary s = summary("0", "0", "0", "0", Optional.empty(), List.of(
                    goal("On track", "450", "1000", LocalDate.of(2026, 4, 19), LocalDate.of(2027, 4, 19), false),
                    goal("Done", "100", "1000", LocalDate.of(2026, 4, 19), LocalDate.of(2027, 4, 19), true),
                    goal("Someday", "0", "1000", LocalDate.of(2026, 4, 19), null, false)));

            assertThat(new SavingsProgressRule().evaluate(s)).isEmpty();
        }
    }

    @Test
    void engineOrdersInsightsMostUrgentFirst() {
        FinancialInsightRule info = s -> Optional.of(new FinancialInsight("B_INFO", InsightSeverity.INFO, "i", "i"));
        FinancialInsightRule alert = s -> Optional.of(new FinancialInsight("Z_ALERT", InsightSeverity.ALERT, "a", "a"));
        FinancialInsightRule none = s -> Optional.empty();
        FinancialInsightRule positive = s -> Optional.of(new FinancialInsight("A_POS", InsightSeverity.POSITIVE, "p", "p"));

        List<FinancialInsight> result = new InsightEngine(List.of(info, none, positive, alert)).evaluate(plain("0", "0"));

        assertThat(result).extracting(FinancialInsight::code).containsExactly("Z_ALERT", "B_INFO", "A_POS");
    }
}
