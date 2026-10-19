package com.nest.mapper;

import com.nest.dto.response.CategoryResponse;
import com.nest.model.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getCategoryType(),
                category.getColor(),
                category.isDefault(),
                category.isArchived());
    }
}
