package com.nest.service;

import com.nest.dto.response.CategorySpendingResponse;
import com.nest.dto.response.DashboardSummaryResponse;
import com.nest.dto.response.MonthlyTrendsResponse;

import java.time.YearMonth;

/** Read-only aggregates for the dashboard. Transfers are excluded and currencies are never mixed. */
public interface DashboardService {

    DashboardSummaryResponse summary(long userId, YearMonth month);

    /** @param currency {@code null} for the user's preferred currency */
    CategorySpendingResponse spendingByCategory(long userId, YearMonth month, String currency);

    /** @param months number of months ending with {@code endMonth}, inclusive */
    MonthlyTrendsResponse monthlyTrends(long userId, YearMonth endMonth, int months, String currency);
}
