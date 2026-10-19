package com.nest.dto.response;

import com.nest.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record TransactionResponse(
        Long id,
        Long accountId,
        String accountName,
        Long categoryId,
        String categoryName,
        TransactionType type,
        BigDecimal amount,
        String currency,
        String description,
        LocalDate transactionDate,
        Instant createdAt,
        Instant updatedAt
) {
}
