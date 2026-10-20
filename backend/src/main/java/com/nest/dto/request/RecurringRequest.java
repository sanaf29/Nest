package com.nest.dto.request;

import com.nest.model.Frequency;
import com.nest.model.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringRequest(
        @NotNull Long accountId,
        @NotNull Long categoryId,
        @NotNull @Schema(allowableValues = {"INCOME", "EXPENSE"}) TransactionType type,
        @NotNull @Positive @Digits(integer = 15, fraction = 4) @Schema(example = "1650.00") BigDecimal amount,
        @NotBlank @Size(max = 255) @Schema(example = "Rent") String description,
        @NotNull @Schema(example = "MONTHLY") Frequency frequency,
        @NotNull @Schema(example = "2026-11-01", description = "First occurrence; its day of month is kept for monthly schedules") LocalDate startDate,
        @Schema(description = "Optional last possible date") LocalDate endDate,
        @Min(0) @Max(30) @Schema(example = "3", description = "Optional: notify this many days before each occurrence") Integer remindDaysBefore
) {
}
