package com.nest.dto.request;

import com.nest.model.TransactionType;
import com.nest.validation.ValidCurrency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Body for creating (POST) and fully replacing (PUT) an income or expense transaction. */
public record TransactionRequest(
        @NotNull
        @Schema(example = "1")
        Long accountId,

        @NotNull
        @Schema(example = "9", description = "A default category or one of the user's own, matching the type.")
        Long categoryId,

        @NotNull
        @Schema(example = "EXPENSE", allowableValues = {"INCOME", "EXPENSE"},
                description = "Transfers are created with POST /api/transfers.")
        TransactionType type,

        @NotNull @Positive @Digits(integer = 15, fraction = 4)
        @Schema(example = "84.37", description = "Always positive; the type gives the direction.")
        BigDecimal amount,

        @ValidCurrency
        @Schema(example = "CAD", description = "Optional. If given, it must equal the account's currency.")
        String currency,

        @Size(max = 255)
        @Schema(example = "Weekly groceries")
        String description,

        @NotNull
        @Schema(example = "2026-10-18")
        LocalDate transactionDate
) {
}
