package com.nest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateGoalRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal targetAmount,
        @Schema(description = "Optional; null removes the deadline") LocalDate targetDate
) {
}
