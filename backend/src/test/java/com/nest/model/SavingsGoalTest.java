package com.nest.model;

import com.nest.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SavingsGoalTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 19);
    private static final Instant NOW = Instant.parse("2026-10-19T12:00:00Z");

    private User owner;

    @BeforeEach
    void setUp() {
        owner = new User("Sanae", "Faghfouri", "sanae@example.com", "$2a$hash", "CAD");
        ReflectionTestUtils.setField(owner, "id", 1L);
    }

    private SavingsGoal goal(String target) {
        return new SavingsGoal(owner, "Emergency fund", "CAD", new BigDecimal(target), TODAY.plusMonths(6), TODAY);
    }

    private static void contribute(SavingsGoal goal, String amount) {
        goal.contribute(new BigDecimal(amount), TODAY, null, null, TODAY, NOW);
    }

    @Nested
    class Creation {

        @Test
        void startsActiveWithNothingSaved() {
            SavingsGoal goal = goal("5000");

            assertThat(goal.getStatus()).isEqualTo(GoalStatus.ACTIVE);
            assertThat(goal.savedAmount()).isEqualTo(Money.zero("CAD"));
            assertThat(goal.progressPercent()).isEqualByComparingTo("0");
            assertThat(goal.remainingAmount()).isEqualTo(Money.of(new BigDecimal("5000"), "CAD"));
        }

        @Test
        void rejectsPastDeadlineNonPositiveTargetAndBlankName() {
            assertThatThrownBy(() -> new SavingsGoal(owner, "Trip", "CAD", BigDecimal.TEN, TODAY.minusDays(1), TODAY))
                    .isInstanceOf(BusinessRuleException.class).hasMessageContaining("past");
            assertThatThrownBy(() -> new SavingsGoal(owner, "Trip", "CAD", BigDecimal.ZERO, null, TODAY))
                    .isInstanceOf(BusinessRuleException.class);
            assertThatThrownBy(() -> new SavingsGoal(owner, " ", "CAD", BigDecimal.TEN, null, TODAY))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void deadlineTodayAndNoDeadlineAreAllowed() {
            assertThat(new SavingsGoal(owner, "Now", "CAD", BigDecimal.TEN, TODAY, TODAY).getTargetDate()).isEqualTo(TODAY);
            assertThat(new SavingsGoal(owner, "Someday", "CAD", BigDecimal.TEN, null, TODAY).getTargetDate()).isNull();
        }
    }

    @Nested
    class Progress {

        @Test
        void contributionsAccumulateAndProgressIsRoundedHalfEven() {
            SavingsGoal goal = goal("3000");
            contribute(goal, "1000");
            contribute(goal, "0.50");

            assertThat(goal.savedAmount()).isEqualTo(Money.of(new BigDecimal("1000.50"), "CAD"));
            assertThat(goal.progressPercent()).isEqualByComparingTo("33.35"); // 33.35 exactly
            assertThat(goal.remainingAmount()).isEqualTo(Money.of(new BigDecimal("1999.50"), "CAD"));
        }

        @Test
        void reachingTheTargetMarksTheGoalAchievedAndCapsProgress() {
            SavingsGoal goal = goal("1000");
            contribute(goal, "600");
            contribute(goal, "500");

            assertThat(goal.getStatus()).isEqualTo(GoalStatus.ACHIEVED);
            assertThat(goal.getAchievedAt()).isEqualTo(NOW);
            assertThat(goal.progressPercent()).isEqualByComparingTo("100");
            assertThat(goal.remainingAmount()).isEqualTo(Money.zero("CAD"));
            assertThat(goal.savedAmount()).isEqualTo(Money.of(new BigDecimal("1100"), "CAD"));
        }

        @Test
        void raisingTheTargetReopensAnAchievedGoal() {
            SavingsGoal goal = goal("1000");
            contribute(goal, "1000");

            goal.update("Emergency fund", new BigDecimal("2000"), null, NOW);

            assertThat(goal.getStatus()).isEqualTo(GoalStatus.ACTIVE);
            assertThat(goal.getAchievedAt()).isNull();
            assertThat(goal.progressPercent()).isEqualByComparingTo("50");
        }

        @Test
        void loweringTheTargetCanAchieveTheGoal() {
            SavingsGoal goal = goal("1000");
            contribute(goal, "800");

            goal.update("Emergency fund", new BigDecimal("800"), null, NOW);

            assertThat(goal.getStatus()).isEqualTo(GoalStatus.ACHIEVED);
        }
    }

    @Nested
    class Contributions {

        @Test
        void rejectsNonPositiveFutureDatedOrOverlyPreciseContributions() {
            SavingsGoal goal = goal("1000");

            assertThatThrownBy(() -> goal.contribute(BigDecimal.ZERO, TODAY, null, null, TODAY, NOW))
                    .isInstanceOf(BusinessRuleException.class);
            assertThatThrownBy(() -> goal.contribute(BigDecimal.ONE, TODAY.plusDays(1), null, null, TODAY, NOW))
                    .isInstanceOf(BusinessRuleException.class).hasMessageContaining("future");
            assertThatThrownBy(() -> goal.contribute(new BigDecimal("1.001"), TODAY, null, null, TODAY, NOW))
                    .isInstanceOf(BusinessRuleException.class);
            assertThat(goal.getContributions()).isEmpty();
        }

        @Test
        void idempotencyKeyCannotBeReused() {
            SavingsGoal goal = goal("1000");
            goal.contribute(BigDecimal.TEN, TODAY, "first", "key-12345", TODAY, NOW);

            assertThat(goal.contributionWithKey("key-12345")).isPresent();
            assertThatThrownBy(() -> goal.contribute(BigDecimal.TEN, TODAY, null, "key-12345", TODAY, NOW))
                    .isInstanceOf(BusinessRuleException.class);
            assertThat(goal.savedAmount()).isEqualTo(Money.of(BigDecimal.TEN, "CAD"));
        }

        @Test
        void removingAContributionReopensTheGoal() {
            SavingsGoal goal = goal("100");
            GoalContribution contribution = goal.contribute(new BigDecimal("100"), TODAY, null, null, TODAY, NOW);
            ReflectionTestUtils.setField(contribution, "id", 5L);
            assertThat(goal.getStatus()).isEqualTo(GoalStatus.ACHIEVED);

            assertThat(goal.removeContribution(5L, NOW)).isTrue();
            assertThat(goal.removeContribution(99L, NOW)).isFalse();

            assertThat(goal.getStatus()).isEqualTo(GoalStatus.ACTIVE);
            assertThat(goal.savedAmount()).isEqualTo(Money.zero("CAD"));
        }
    }
}
