package com.nest.dto.response;

import com.nest.model.ReminderStatus;
import com.nest.model.ReminderType;

import java.time.Instant;

public record ReminderResponse(
        Long id,
        ReminderType type,
        String title,
        String notes,
        Instant dueAt,
        int offsetMinutes,
        Instant remindAt,
        ReminderStatus status,
        Instant sentAt,
        String location,
        Long savingsGoalId,
        String savingsGoalName,
        Long categoryId,
        String categoryName,
        Integer thresholdPercent,
        Instant createdAt
) {
}
