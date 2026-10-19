package com.nest.dto.response;

import com.nest.model.SavingsStrategyType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BudgetResponse(
        Long id,
        int year,
        int month,
        String currency,
        BigDecimal plannedIncome,
        SavingsStrategyType savingsStrategy,
        BigDecimal savingsValue,
        @Schema(description = "Derived from planned income and the savings strategy") BigDecimal savingsTarget,
        @Schema(description = "Planned income − savings target") BigDecimal spendableAmount,
        BigDecimal allocatedAmount,
        BigDecimal unallocatedAmount,
        List<Allocation> allocations,
        Instant createdAt,
        Instant updatedAt
) {
    public record Allocation(Long id, Long categoryId, String categoryName, String color, boolean categoryArchived,
                             BigDecimal limit) {
    }
}
