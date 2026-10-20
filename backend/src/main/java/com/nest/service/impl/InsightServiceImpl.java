package com.nest.service.impl;

import com.nest.dto.response.InsightsResponse;
import com.nest.exception.ResourceNotFoundException;
import com.nest.model.GoalStatus;
import com.nest.model.MonthlyActuals;
import com.nest.model.SavingsGoal;
import com.nest.repository.MonthlyBudgetRepository;
import com.nest.repository.SavingsGoalRepository;
import com.nest.repository.UserRepository;
import com.nest.service.InsightService;
import com.nest.service.LedgerAnalytics;
import com.nest.service.insight.FinancialSummary;
import com.nest.service.insight.InsightEngine;
import com.nest.service.insight.RecurringChangeSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;

/** Gathers the data the rules need into a {@link FinancialSummary}, then runs the engine. */
@Service
public class InsightServiceImpl implements InsightService {

    static final String DISCLAIMER =
            "Insights are automatic observations based on your own records. They are informational and are not financial advice.";

    private final LedgerAnalytics ledgerAnalytics;
    private final MonthlyBudgetRepository budgetRepository;
    private final SavingsGoalRepository goalRepository;
    private final UserRepository userRepository;
    private final RecurringChangeSource recurringChanges;
    private final InsightEngine engine;
    private final Clock clock;

    public InsightServiceImpl(LedgerAnalytics ledgerAnalytics, MonthlyBudgetRepository budgetRepository,
                              SavingsGoalRepository goalRepository, UserRepository userRepository,
                              RecurringChangeSource recurringChanges, InsightEngine engine, Clock clock) {
        this.ledgerAnalytics = ledgerAnalytics;
        this.budgetRepository = budgetRepository;
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
        this.recurringChanges = recurringChanges;
        this.engine = engine;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public InsightsResponse insights(long userId, YearMonth month, String currency) {
        String effective = currency != null ? currency
                : userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId)).getPreferredCurrency();
        FinancialSummary summary = summarize(userId, month, effective);
        return new InsightsResponse(month.getYear(), month.getMonthValue(), effective, summary.asOf(), DISCLAIMER,
                engine.evaluate(summary).stream()
                        .map(i -> new InsightsResponse.Insight(i.code(), i.severity(), i.title(), i.message()))
                        .toList());
    }

    FinancialSummary summarize(long userId, YearMonth month, String currency) {
        LocalDate today = LocalDate.now(clock);
        LocalDate asOf = month.equals(YearMonth.from(today)) ? today : month.atEndOfMonth();
        YearMonth previous = month.minusMonths(1);
        // Same number of days last month, clamped to its length (e.g. 31 March → 28/29 February).
        LocalDate previousTo = previous.atDay(Math.min(asOf.getDayOfMonth(), previous.lengthOfMonth()));

        MonthlyActuals actuals = ledgerAnalytics.monthlyActuals(userId, month, currency);
        return new FinancialSummary(
                month,
                currency,
                asOf,
                actuals.income(),
                actuals.totalExpenses(),
                ledgerAnalytics.totalExpenses(userId, currency, month.atDay(1), asOf),
                ledgerAnalytics.totalExpenses(userId, currency, previous.atDay(1), previousTo),
                budgetRepository.findByUserIdAndYearAndMonthAndCurrency(userId, month.getYear(), month.getMonthValue(), currency)
                        .map(budget -> budget.summarize(actuals)),
                goalRepository.findByUserIdOrderByStatusDescTargetDateAscNameAsc(userId).stream()
                        .map(InsightServiceImpl::snapshot)
                        .toList(),
                recurringChanges.recentChanges(userId, currency));
    }

    private static FinancialSummary.GoalSnapshot snapshot(SavingsGoal goal) {
        return new FinancialSummary.GoalSnapshot(goal.getName(), goal.savedAmount(), goal.target(),
                LocalDate.ofInstant(goal.getCreatedAt(), ZoneOffset.UTC), goal.getTargetDate(), goal.progressPercent(),
                goal.getStatus() == GoalStatus.ACHIEVED);
    }
}
