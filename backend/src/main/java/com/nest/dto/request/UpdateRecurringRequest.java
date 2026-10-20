package com.nest.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Changes apply to future occurrences. Type, frequency, and start date are fixed. */
public record UpdateRecurringRequest(
        @NotNull Long accountId,
        @NotNull Long categoryId,
        @NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal amount,
        @NotBlank @Size(max = 255) String description,
        LocalDate endDate,
        @Min(0) @Max(30) Integer remindDaysBefore
) {
}
