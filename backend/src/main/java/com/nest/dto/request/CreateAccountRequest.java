package com.nest.dto.request;

import com.nest.model.AccountType;
import com.nest.validation.ValidCurrency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank @Size(max = 100)
        @Schema(example = "Everyday Chequing")
        String name,

        @NotNull
        @Schema(example = "CHECKING")
        AccountType accountType,

        @ValidCurrency
        @Schema(example = "CAD", description = "Optional; defaults to the user's preferred currency. Cannot be changed later.")
        String currency,

        // Structural limit only; the currency-specific limit (e.g. 2 decimals for CAD) is a domain rule (422).
        @NotNull @Digits(integer = 15, fraction = 4)
        @Schema(example = "1500.00", description = "Starting balance. Negative is allowed only for CHECKING and CREDIT_CARD.")
        BigDecimal openingBalance
) {
}
