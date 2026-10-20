package com.nest.dto.request;

import com.nest.model.SplitMethod;
import com.nest.validation.ValidCurrency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SharedExpenseRequest(
        @NotBlank @Size(max = 150) @Schema(example = "Cabin weekend") String description,
        @NotNull @Positive @Digits(integer = 15, fraction = 4) @Schema(example = "300.00") BigDecimal totalAmount,
        @ValidCurrency @Schema(example = "CAD", description = "Optional; defaults to your preferred currency") String currency,
        @NotNull LocalDate expenseDate,
        @NotNull @Schema(example = "EQUAL") SplitMethod splitMethod,
        @NotNull @Size(min = 2, max = 20) List<@Valid @NotNull Participant> participants
) {
    public record Participant(
            @NotBlank @Size(max = 100) @Schema(example = "Alex") String name,
            @Email @Size(max = 254) String email,
            @Schema(description = "This participant is you (exactly one)") boolean me,
            @Schema(description = "This participant paid the bill (exactly one)") boolean paid,
            @Digits(integer = 15, fraction = 4)
            @Schema(description = "CUSTOM: amount; PERCENTAGE: percent; EQUAL: ignored") BigDecimal value
    ) {
    }
}
