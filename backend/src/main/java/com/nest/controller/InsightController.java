package com.nest.controller;

import com.nest.dto.response.InsightsResponse;
import com.nest.security.CurrentUserProvider;
import com.nest.service.InsightService;
import com.nest.validation.ValidCurrency;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/insights")
@Tag(name = "Financial Insights")
public class InsightController {

    private final InsightService insightService;
    private final CurrentUserProvider currentUser;
    private final Clock clock;

    public InsightController(InsightService insightService, CurrentUserProvider currentUser, Clock clock) {
        this.insightService = insightService;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @GetMapping
    @Operation(summary = "Rule-based insights for a month",
            description = "Deterministic and explainable: each insight states the numbers behind it. Not financial advice.")
    public InsightsResponse insights(@RequestParam(required = false) @Min(2000) @Max(2100) Integer year,
                                     @RequestParam(required = false) @Min(1) @Max(12) Integer month,
                                     @RequestParam(required = false) @ValidCurrency String currency) {
        YearMonth period = year != null && month != null ? YearMonth.of(year, month) : YearMonth.now(clock);
        return insightService.insights(currentUser.currentUserId(), period, currency);
    }
}
