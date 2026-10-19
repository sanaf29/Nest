package com.nest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Only the name and colour are editable. The type is fixed so past transactions stay correctly classified. */
public record UpdateCategoryRequest(
        @NotBlank @Size(max = 50)
        @Schema(example = "Pet Care")
        String name,

        @Pattern(regexp = "#[0-9A-Fa-f]{6}", message = "must be a hex colour like #1A2B3C")
        @Schema(example = "#EA580C", description = "Optional; omitted resets to grey.")
        String color
) {
}
