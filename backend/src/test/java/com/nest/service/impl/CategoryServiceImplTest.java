package com.nest.service.impl;

import com.nest.dto.request.CreateCategoryRequest;
import com.nest.dto.request.UpdateCategoryRequest;
import com.nest.dto.response.CategoryResponse;
import com.nest.exception.ConflictException;
import com.nest.exception.ForbiddenOperationException;
import com.nest.exception.ResourceNotFoundException;
import com.nest.mapper.CategoryMapper;
import com.nest.model.Category;
import com.nest.model.CategoryFixtures;
import com.nest.model.CategoryType;
import com.nest.model.User;
import com.nest.repository.CategoryRepository;
import com.nest.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    private static final long USER_ID = 1L;

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;

    private CategoryServiceImpl service;
    private User owner;

    @BeforeEach
    void setUp() {
        service = new CategoryServiceImpl(categoryRepository, userRepository, new CategoryMapper());
        owner = new User("Sanae", "Faghfouri", "sanae@example.com", "$2a$hash", "CAD");
        ReflectionTestUtils.setField(owner, "id", USER_ID);
    }

    private Category ownCategory(long id) {
        Category category = new Category(owner, "Pets", CategoryType.EXPENSE, "#F97316");
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    @Test
    void createsCategoryForTheUser() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponse response = service.create(USER_ID, new CreateCategoryRequest(" Pets ", CategoryType.EXPENSE, null));

        verify(categoryRepository).existsActiveVisibleName(USER_ID, "Pets", CategoryType.EXPENSE, null);
        assertThat(response.name()).isEqualTo("Pets");
        assertThat(response.isDefault()).isFalse();
        assertThat(response.color()).isEqualTo(Category.DEFAULT_COLOR);
    }

    @Test
    void rejectsNameThatDuplicatesADefaultOrOwnCategory() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
        when(categoryRepository.existsActiveVisibleName(USER_ID, "groceries", CategoryType.EXPENSE, null)).thenReturn(true);

        assertThatThrownBy(() -> service.create(USER_ID, new CreateCategoryRequest("groceries", CategoryType.EXPENSE, null)))
                .isInstanceOf(ConflictException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void updatingADefaultIsForbiddenWithoutQueryingNames() {
        Category salary = CategoryFixtures.defaultCategory(5L, "Salary", CategoryType.INCOME);
        when(categoryRepository.findVisible(5L, USER_ID)).thenReturn(Optional.of(salary));

        assertThatThrownBy(() -> service.update(USER_ID, 5L, new UpdateCategoryRequest("Wages", null)))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(categoryRepository, never()).existsActiveVisibleName(anyLong(), anyString(), any(), any());
        verify(categoryRepository, never()).saveAndFlush(any());
    }

    @Test
    void duplicateRenameIsRejectedBeforeTheEntityChanges() {
        Category category = ownCategory(20L);
        when(categoryRepository.findVisible(20L, USER_ID)).thenReturn(Optional.of(category));
        when(categoryRepository.existsActiveVisibleName(USER_ID, "Groceries", CategoryType.EXPENSE, 20L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(USER_ID, 20L, new UpdateCategoryRequest("Groceries", null)))
                .isInstanceOf(ConflictException.class);
        assertThat(category.getName()).isEqualTo("Pets");
    }

    @Test
    void renamesOwnCategory() {
        Category category = ownCategory(20L);
        when(categoryRepository.findVisible(20L, USER_ID)).thenReturn(Optional.of(category));
        when(categoryRepository.saveAndFlush(category)).thenReturn(category);

        CategoryResponse response = service.update(USER_ID, 20L, new UpdateCategoryRequest("Pet Care", "#000000"));

        assertThat(response.name()).isEqualTo("Pet Care");
        assertThat(response.color()).isEqualTo("#000000");
    }

    @Test
    void anotherUsersCategoryIsNotFound() {
        when(categoryRepository.findVisible(20L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.archive(2L, 20L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.get(2L, 20L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void archivesOwnCategory() {
        Category category = ownCategory(20L);
        when(categoryRepository.findVisible(20L, USER_ID)).thenReturn(Optional.of(category));

        service.archive(USER_ID, 20L);

        assertThat(category.isArchived()).isTrue();
    }

    @Test
    void listDelegatesFiltersToTheOwnerScopedQuery() {
        when(categoryRepository.findVisible(USER_ID, CategoryType.INCOME, false))
                .thenReturn(List.of(CategoryFixtures.defaultCategory(1L, "Salary", CategoryType.INCOME)));

        assertThat(service.list(USER_ID, CategoryType.INCOME, false))
                .singleElement()
                .satisfies(c -> assertThat(c.isDefault()).isTrue());
    }
}
