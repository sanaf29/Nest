package com.nest.service.impl;

import com.nest.dto.request.CreateCategoryRequest;
import com.nest.dto.request.UpdateCategoryRequest;
import com.nest.dto.response.CategoryResponse;
import com.nest.exception.ConflictException;
import com.nest.exception.ResourceNotFoundException;
import com.nest.mapper.CategoryMapper;
import com.nest.model.Category;
import com.nest.model.CategoryType;
import com.nest.model.User;
import com.nest.repository.CategoryRepository;
import com.nest.repository.UserRepository;
import com.nest.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryRepository categoryRepository,
                               UserRepository userRepository,
                               CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.categoryMapper = categoryMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> list(long userId, CategoryType type, boolean includeArchived) {
        return categoryRepository.findVisible(userId, type, includeArchived).stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse get(long userId, long categoryId) {
        return categoryMapper.toResponse(findVisible(userId, categoryId));
    }

    @Override
    @Transactional
    public CategoryResponse create(long userId, CreateCategoryRequest request) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        requireUniqueName(userId, request.name(), request.categoryType(), null);
        Category category = new Category(owner, request.name(), request.categoryType(), request.color());
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse update(long userId, long categoryId, UpdateCategoryRequest request) {
        Category category = findVisible(userId, categoryId);
        // Check uniqueness before mutating, so Hibernate's auto-flush before the query cannot write
        // an unchecked name. Defaults skip it: category.update() rejects them with 403 regardless of name.
        if (!category.isDefault()) {
            requireUniqueName(userId, request.name(), category.getCategoryType(), categoryId);
        }
        category.update(request.name(), request.color());
        return categoryMapper.toResponse(categoryRepository.saveAndFlush(category));
    }

    @Override
    @Transactional
    public void archive(long userId, long categoryId) {
        findVisible(userId, categoryId).archive();
    }

    private Category findVisible(long userId, long categoryId) {
        return categoryRepository.findVisible(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
    }

    /** Names are unique per type across the user's own categories and the defaults, ignoring case. */
    private void requireUniqueName(long userId, String name, CategoryType type, Long excludedId) {
        String trimmed = name.strip();
        if (categoryRepository.existsActiveVisibleName(userId, trimmed, type, excludedId)) {
            throw new ConflictException("An %s category named '%s' already exists."
                    .formatted(type.name().toLowerCase(Locale.ROOT), trimmed));
        }
    }
}
