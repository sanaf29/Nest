package com.nest.dto.request;

import com.nest.model.CategoryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank @Size(max = 50)
        @Schema(example = "Pets")
        String name,

        @NotNull
        @Schema(example = "EXPENSE", description = "Cannot be changed later.")
        CategoryType categoryType,

        @Pattern(regexp = "#[0-9A-Fa-f]{6}", message = "must be a hex colour like #1A2B3C")
        @Schema(example = "#F97316", description = "Optional; defaults to grey.")
        String color
) {
}
