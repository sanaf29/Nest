package com.nest.dto.request;

import com.nest.model.SavingsStrategyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** Replaces the plan and all allocations. The period and currency cannot change. */
public record UpdateBudgetRequest(
        @NotNull @PositiveOrZero @Digits(integer = 15, fraction = 4) @Schema(example = "3200.00")
        BigDecimal plannedIncome,
        @NotNull SavingsStrategyType savingsStrategy,
        @NotNull @PositiveOrZero @Digits(integer = 15, fraction = 4) BigDecimal savingsValue,
        @Size(max = 100) List<@Valid @NotNull AllocationRequest> allocations
) {
    public List<AllocationRequest> allocationsOrEmpty() {
        return allocations == null ? List.of() : allocations;
    }
}
