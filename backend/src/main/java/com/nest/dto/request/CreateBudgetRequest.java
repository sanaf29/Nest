package com.nest.dto.request;

import com.nest.model.SavingsStrategyType;
import com.nest.validation.ValidCurrency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateBudgetRequest(
        @NotNull @Min(2000) @Max(2100) @Schema(example = "2026") Integer year,
        @NotNull @Min(1) @Max(12) @Schema(example = "10") Integer month,

        @ValidCurrency
        @Schema(example = "CAD", description = "Optional; defaults to your preferred currency. Fixed once created.")
        String currency,

        @NotNull @PositiveOrZero @Digits(integer = 15, fraction = 4) @Schema(example = "3000.00")
        BigDecimal plannedIncome,

        @NotNull @Schema(example = "PERCENTAGE") SavingsStrategyType savingsStrategy,

        @NotNull @PositiveOrZero @Digits(integer = 15, fraction = 4)
        @Schema(example = "20", description = "A percentage (0–100) for PERCENTAGE, or an amount for FIXED_AMOUNT")
        BigDecimal savingsValue,

        @Size(max = 100) List<@Valid @NotNull AllocationRequest> allocations
) {
    public List<AllocationRequest> allocationsOrEmpty() {
        return allocations == null ? List.of() : allocations;
    }
}
