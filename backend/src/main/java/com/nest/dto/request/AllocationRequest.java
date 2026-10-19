package com.nest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AllocationRequest(
        @NotNull @Schema(example = "9", description = "An EXPENSE category (default or your own)") Long categoryId,
        @NotNull @Positive @Digits(integer = 15, fraction = 4) @Schema(example = "450.00") BigDecimal limit
) {
}
