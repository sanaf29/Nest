package com.nest.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record TransferResponse(
        Long id,
        Long sourceAccountId,
        String sourceAccountName,
        Long destinationAccountId,
        String destinationAccountName,
        BigDecimal amount,
        String currency,
        LocalDate transferDate,
        Long outgoingTransactionId,
        Long incomingTransactionId,
        Instant createdAt
) {
}
