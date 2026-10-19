package com.nest.dto.request;

import com.nest.model.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Optional query-string filters for GET /api/transactions. All are combined with AND. */
public record TransactionFilter(
        Long accountId,
        Long categoryId,
        TransactionType type,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Schema(example = "2026-10-01") LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Schema(example = "2026-10-31") LocalDate to,
        @PositiveOrZero BigDecimal minAmount,
        @PositiveOrZero BigDecimal maxAmount,
        @Size(max = 100) @Schema(description = "Case-insensitive text search in the description") String q
) {
    @AssertTrue(message = "'from' must not be after 'to'")
    @Schema(hidden = true)
    public boolean isDateRangeValid() {
        return from == null || to == null || !from.isAfter(to);
    }

    @AssertTrue(message = "'minAmount' must not exceed 'maxAmount'")
    @Schema(hidden = true)
    public boolean isAmountRangeValid() {
        return minAmount == null || maxAmount == null || minAmount.compareTo(maxAmount) <= 0;
    }
}
