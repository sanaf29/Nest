package com.nest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Only the name and opening balance are editable. The type and currency are fixed once the account exists. */
public record UpdateAccountRequest(
        @NotBlank @Size(max = 100)
        @Schema(example = "Main Chequing")
        String name,

        @NotNull @Digits(integer = 15, fraction = 4)
        @Schema(example = "1450.00")
        BigDecimal openingBalance
) {
}
