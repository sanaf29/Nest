package com.nest.dto.request;

import com.nest.validation.ValidCurrency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Full replacement of the editable profile fields (PUT semantics).
 * Email and password changes are separate, security-sensitive flows and are not part of this request.
 */
public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) @Schema(example = "Sanae") String firstName,
        @NotBlank @Size(max = 100) @Schema(example = "Faghfouri") String lastName,
        @NotNull @ValidCurrency @Schema(example = "CAD") String preferredCurrency
) {
}
