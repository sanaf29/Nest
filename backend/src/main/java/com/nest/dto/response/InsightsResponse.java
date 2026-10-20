package com.nest.dto.response;

import com.nest.service.insight.InsightSeverity;

import java.time.LocalDate;
import java.util.List;

public record InsightsResponse(int year, int month, String currency, LocalDate asOf, String disclaimer, List<Insight> insights) {

    public record Insight(String code, InsightSeverity severity, String title, String message) {
    }
}
