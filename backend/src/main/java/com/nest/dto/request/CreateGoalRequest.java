package com.nest.dto.request;

import com.nest.validation.ValidCurrency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateGoalRequest(
        @NotBlank @Size(max = 100) @Schema(example = "Emergency fund") String name,
        @NotNull @Positive @Digits(integer = 15, fraction = 4) @Schema(example = "5000.00") BigDecimal targetAmount,
        @ValidCurrency @Schema(example = "CAD", description = "Optional; defaults to your preferred currency. Fixed once created.")
        String currency,
        @Schema(example = "2027-06-30", description = "Optional deadline; not in the past") LocalDate targetDate
) {
}
