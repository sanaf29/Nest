package com.nest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransferRequest(
        @NotNull @Schema(example = "1") Long sourceAccountId,
        @NotNull @Schema(example = "2") Long destinationAccountId,

        @NotNull @Positive @Digits(integer = 15, fraction = 4)
        @Schema(example = "500.00", description = "In the accounts' shared currency.")
        BigDecimal amount,

        @Size(max = 255)
        @Schema(example = "Monthly savings", description = "Optional; defaults to 'Transfer to/from <account>'.")
        String description,

        @NotNull @Schema(example = "2026-10-19") LocalDate transferDate
) {
}
