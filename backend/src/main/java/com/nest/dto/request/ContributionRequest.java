package com.nest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContributionRequest(
        @NotNull @Positive @Digits(integer = 15, fraction = 4) @Schema(example = "250.00") BigDecimal amount,
        @Schema(example = "2026-10-19", description = "Optional; defaults to today. Not in the future.") LocalDate contributionDate,
        @Size(max = 255) @Schema(example = "October top-up") String note,
        @Size(min = 8, max = 64) @Pattern(regexp = "[A-Za-z0-9_-]+")
        @Schema(example = "3f0b6c1e-6d2a-4a8e-9a43-2d1f5f0c7b11",
                description = "Optional client-generated key (e.g. a UUID). Resending the same key returns the "
                        + "original result (200) instead of counting the contribution twice.")
        String idempotencyKey
) {
}
