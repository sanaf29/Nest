package com.nest.dto.request;

import com.nest.model.ReminderType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record ReminderRequest(
        @NotNull @Schema(example = "PARKING") ReminderType type,
        @NotBlank @Size(max = 150) @Schema(example = "Move the car") String title,
        @Size(max = 500) String notes,
        @Schema(example = "2026-10-20T15:30:00Z", description = "When it happens (bill due, parking expires). Not for BUDGET_THRESHOLD.")
        Instant dueAt,
        @Min(0) @Max(43200) @Schema(example = "15", description = "Notify this many minutes before dueAt") Integer offsetMinutes,
        @Size(max = 200) @Schema(example = "Level 2, spot B14", description = "Required for PARKING") String location,
        @Schema(description = "Required for SAVINGS_GOAL_DEADLINE") Long savingsGoalId,
        @Schema(description = "Optional for BUDGET_THRESHOLD: watch one expense category instead of the whole budget") Long categoryId,
        @Min(1) @Max(200) @Schema(example = "80", description = "Required for BUDGET_THRESHOLD") Integer thresholdPercent
) {
}
