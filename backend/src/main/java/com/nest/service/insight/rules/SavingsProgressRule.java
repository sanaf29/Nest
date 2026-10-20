package com.nest.service.insight.rules;

import com.nest.model.Money;
import com.nest.service.insight.FinancialInsight;
import com.nest.service.insight.FinancialInsightRule;
import com.nest.service.insight.FinancialSummary;
import com.nest.service.insight.InsightSeverity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * A goal is behind schedule when its saved percentage trails the share of time elapsed between
 * creation and its target date by more than 10 points. The message says how much per month would
 * get it back on track.
 */
@Component
public class SavingsProgressRule implements FinancialInsightRule {

    static final BigDecimal TOLERANCE_POINTS = BigDecimal.TEN;

    @Override
    public Optional<FinancialInsight> evaluate(FinancialSummary summary) {
        List<Behind> behind = summary.goals().stream()
                .filter(g -> !g.achieved() && g.targetDate() != null && g.targetDate().isAfter(g.startedOn()))
                .map(g -> new Behind(g, expectedPercent(g.startedOn(), g.targetDate(), summary.asOf())))
                .filter(b -> b.goal().progressPercent().add(TOLERANCE_POINTS).compareTo(b.expected()) < 0)
                .sorted(Comparator.comparing((Behind b) -> b.expected().subtract(b.goal().progressPercent())).reversed())
                .toList();
        if (behind.isEmpty()) {
            return Optional.empty();
        }
        Behind worst = behind.getFirst();
        FinancialSummary.GoalSnapshot goal = worst.goal();
        Money remaining = goal.target().minus(goal.saved());
        long monthsLeft = Math.max(1, ChronoUnit.MONTHS.between(summary.asOf().withDayOfMonth(1), goal.targetDate().withDayOfMonth(1)));
        Money perMonth = Money.of(remaining.amount().divide(BigDecimal.valueOf(monthsLeft),
                remaining.amount().scale(), RoundingMode.CEILING), remaining.currency());
        String title = behind.size() == 1
                ? "'%s' is behind schedule".formatted(goal.name())
                : "%d goals are behind schedule".formatted(behind.size());
        return Optional.of(new FinancialInsight("GOAL_BEHIND_SCHEDULE", InsightSeverity.WARNING, title,
                "'%s' is %s funded but %s of the time to %s has passed. Saving about %s a month would reach %s on time."
                        .formatted(goal.name(), Percentages.whole(goal.progressPercent()), Percentages.whole(worst.expected()),
                                goal.targetDate(), perMonth, goal.target())));
    }

    /** Share of the period between start and target that has elapsed as of {@code asOf}, 0–100. */
    public static BigDecimal expectedPercent(LocalDate start, LocalDate target, LocalDate asOf) {
        long total = ChronoUnit.DAYS.between(start, target);
        long elapsed = Math.clamp(ChronoUnit.DAYS.between(start, asOf), 0, total);
        return BigDecimal.valueOf(elapsed * 100).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_EVEN);
    }

    private record Behind(FinancialSummary.GoalSnapshot goal, BigDecimal expected) {
    }
}
