package com.nest.dto.response;

import com.nest.model.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        Long id,
        String name,
        AccountType accountType,
        String currency,
        BigDecimal openingBalance,
        @Schema(description = "Derived: opening balance + income + transfers in − expenses − transfers out. "
                + "Negative means money owed (e.g. a credit card).")
        BigDecimal currentBalance,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
