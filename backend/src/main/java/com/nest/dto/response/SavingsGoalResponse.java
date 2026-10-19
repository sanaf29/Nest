package com.nest.dto.response;

import com.nest.model.GoalStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SavingsGoalResponse(
        Long id,
        String name,
        String currency,
        BigDecimal targetAmount,
        @Schema(description = "Sum of contributions") BigDecimal savedAmount,
        BigDecimal remainingAmount,
        @Schema(description = "0–100, capped at 100") BigDecimal progressPercent,
        LocalDate targetDate,
        GoalStatus status,
        Instant achievedAt,
        List<Contribution> contributions,
        Instant createdAt
) {
    public record Contribution(Long id, BigDecimal amount, LocalDate contributionDate, String note, Instant createdAt) {
    }
}
