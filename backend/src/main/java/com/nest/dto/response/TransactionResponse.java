package com.nest.dto.response;

import com.nest.model.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

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
        @Schema(description = "Set on TRANSFER_IN/TRANSFER_OUT entries; change those through /api/transfers/{transferId}.")
        Long transferId,
        Instant createdAt,
        Instant updatedAt
) {
}
