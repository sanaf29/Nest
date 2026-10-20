package com.nest.dto.response;

import com.nest.model.Frequency;
import com.nest.model.RecurringStatus;
import com.nest.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RecurringResponse(
        Long id,
        Long accountId,
        String accountName,
        Long categoryId,
        String categoryName,
        TransactionType type,
        BigDecimal amount,
        String currency,
        String description,
        Frequency frequency,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate nextOccurrence,
        List<LocalDate> upcoming,
        RecurringStatus status,
        String pauseReason,
        Integer remindDaysBefore,
        Instant createdAt
) {
}
