package com.nest.controller;

import com.nest.dto.request.CreateCategoryRequest;
import com.nest.dto.request.UpdateCategoryRequest;
import com.nest.dto.response.CategoryResponse;
import com.nest.model.CategoryType;
import com.nest.security.CurrentUserProvider;
import com.nest.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final CurrentUserProvider currentUser;

    public CategoryController(CategoryService categoryService, CurrentUserProvider currentUser) {
        this.categoryService = categoryService;
        this.currentUser = currentUser;
    }

    @GetMapping
    @Operation(summary = "List default categories and my own",
            description = "Optional filter by type. Archived categories are hidden unless includeArchived=true.")
    public List<CategoryResponse> list(@RequestParam(required = false) CategoryType type,
                                       @RequestParam(defaultValue = "false") boolean includeArchived) {
        return categoryService.list(currentUser.currentUserId(), type, includeArchived);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a default category or one of my own", description = "404 for another user's category.")
    public CategoryResponse get(@PathVariable long id) {
        return categoryService.get(currentUser.currentUserId(), id);
    }

    @PostMapping
    @Operation(summary = "Create a category",
            description = "201 created; 400 invalid input; 409 a default or own category with this name and type exists.")
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse created = categoryService.create(currentUser.currentUserId(), request);
        return ResponseEntity.created(URI.create("/api/categories/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Rename or recolour one of my categories",
            description = "403 for default categories; 409 duplicate name; 422 archived category.")
    public CategoryResponse update(@PathVariable long id, @Valid @RequestBody UpdateCategoryRequest request) {
        return categoryService.update(currentUser.currentUserId(), id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Archive one of my categories",
            description = "Soft delete: hidden from new use, kept on past transactions. Idempotent. 403 for defaults.")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        categoryService.archive(currentUser.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
