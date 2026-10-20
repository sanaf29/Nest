package com.nest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** Net position with one person in one currency, across unsettled shared expenses. */
public record SharedBalanceResponse(
        String person,
        String currency,
        @Schema(description = "Positive: they owe you. Negative: you owe them.") BigDecimal amount
) {
}
