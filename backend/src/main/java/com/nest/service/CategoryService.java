package com.nest.service;

import com.nest.dto.request.CreateCategoryRequest;
import com.nest.dto.request.UpdateCategoryRequest;
import com.nest.dto.response.CategoryResponse;
import com.nest.model.CategoryType;

import java.util.List;

/**
 * Default and user-defined categories. A user sees the defaults plus their own, and can modify
 * only their own. Another user's category behaves as if it does not exist.
 */
public interface CategoryService {

    /** @param type {@code null} for both types */
    List<CategoryResponse> list(long userId, CategoryType type, boolean includeArchived);

    CategoryResponse get(long userId, long categoryId);

    CategoryResponse create(long userId, CreateCategoryRequest request);

    CategoryResponse update(long userId, long categoryId, UpdateCategoryRequest request);

    /** Archives a user-defined category. Past transactions keep it. */
    void archive(long userId, long categoryId);
}
