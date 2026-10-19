package com.nest.controller;

import com.nest.dto.response.CategorySpendingResponse;
import com.nest.dto.response.DashboardSummaryResponse;
import com.nest.dto.response.MonthlyTrendsResponse;
import com.nest.security.CurrentUserProvider;
import com.nest.service.DashboardService;
import com.nest.validation.ValidCurrency;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.YearMonth;

/**
 * The month defaults to the current UTC month. Clients in other time zones should pass
 * {@code year} and {@code month} explicitly (the web app always does).
 */
@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentUserProvider currentUser;
    private final Clock clock;

    public DashboardController(DashboardService dashboardService, CurrentUserProvider currentUser, Clock clock) {
        this.dashboardService = dashboardService;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @GetMapping("/summary")
    @Operation(summary = "Balances, cash flow, budget, and goal progress for a month",
            description = "All totals are grouped by currency. Transfers are excluded from income and expenses.")
    public DashboardSummaryResponse summary(@RequestParam(required = false) @Min(2000) @Max(2100) Integer year,
                                            @RequestParam(required = false) @Min(1) @Max(12) Integer month) {
        return dashboardService.summary(currentUser.currentUserId(), period(year, month));
    }

    @GetMapping("/spending-by-category")
    @Operation(summary = "Expenses for a month by category, largest first")
    public CategorySpendingResponse spendingByCategory(@RequestParam(required = false) @Min(2000) @Max(2100) Integer year,
                                                       @RequestParam(required = false) @Min(1) @Max(12) Integer month,
                                                       @RequestParam(required = false) @ValidCurrency String currency) {
        return dashboardService.spendingByCategory(currentUser.currentUserId(), period(year, month), currency);
    }

    @GetMapping("/monthly-trends")
    @Operation(summary = "Income, expenses, and net per month", description = "Oldest first; empty months included as zero.")
    public MonthlyTrendsResponse monthlyTrends(
            @Parameter(description = "Number of months, 1–24, ending with year/month") @RequestParam(defaultValue = "6") @Min(1) @Max(24) int months,
            @RequestParam(required = false) @Min(2000) @Max(2100) Integer year,
            @RequestParam(required = false) @Min(1) @Max(12) Integer month,
            @RequestParam(required = false) @ValidCurrency String currency) {
        return dashboardService.monthlyTrends(currentUser.currentUserId(), period(year, month), months, currency);
    }

    /** Both or neither: a year without a month (or vice versa) falls back to the current month. */
    private YearMonth period(Integer year, Integer month) {
        return year != null && month != null ? YearMonth.of(year, month) : YearMonth.now(clock);
    }
}
