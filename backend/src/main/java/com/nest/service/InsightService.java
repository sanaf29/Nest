package com.nest.service;

import com.nest.dto.response.InsightsResponse;

import java.time.YearMonth;

public interface InsightService {

    /** @param currency {@code null} for the user's preferred currency */
    InsightsResponse insights(long userId, YearMonth month, String currency);
}
