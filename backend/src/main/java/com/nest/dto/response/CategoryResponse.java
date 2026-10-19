package com.nest.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nest.model.CategoryType;
import io.swagger.v3.oas.annotations.media.Schema;

public record CategoryResponse(
        Long id,
        String name,
        CategoryType categoryType,
        String color,
        @JsonProperty("isDefault")
        @Schema(description = "System category: visible to everyone, cannot be edited or deleted.")
        boolean isDefault,
        boolean archived
) {
}
