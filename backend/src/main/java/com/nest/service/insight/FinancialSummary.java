package com.nest.service.insight;

import com.nest.model.BudgetSummary;
import com.nest.model.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Read-only snapshot of one user's finances for a month and currency: everything the insight
 * rules may look at. Rules are pure functions of this value, so each is unit-tested with a
 * hand-built summary and no Spring or database.
 *
 * @param asOf                       last day considered: today for the current month, otherwise the month's last day
 * @param expensesToDate             expenses from the 1st up to {@code asOf}
 * @param expensesSamePeriodLastMonth expenses in the previous month over the same number of days, so a
 *                                   partial month is never compared with a full one
 */
public record FinancialSummary(
        YearMonth month,
        String currency,
        LocalDate asOf,
        Money income,
        Money expenses,
        Money expensesToDate,
        Money expensesSamePeriodLastMonth,
        Optional<BudgetSummary> budget,
        List<GoalSnapshot> goals,
        List<RecurringChange> recurringChanges
) {

    /** An active or achieved goal, in its own currency. */
    public record GoalSnapshot(String name, Money saved, Money target, LocalDate startedOn, LocalDate targetDate,
                               BigDecimal progressPercent, boolean achieved) {
    }

    /** A recurring expense whose two most recent generated amounts differ. */
    public record RecurringChange(String description, Money previousAmount, Money latestAmount) {
    }
}
