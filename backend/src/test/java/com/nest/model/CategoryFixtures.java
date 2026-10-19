package com.nest.model;

import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

/** Builds a default category the way the migration does (no owner), which production code cannot do. */
public final class CategoryFixtures {

    private CategoryFixtures() {
    }

    public static Category defaultCategory(long id, String name, CategoryType type) {
        Category category = BeanUtils.instantiateClass(Category.class);
        ReflectionTestUtils.setField(category, "id", id);
        ReflectionTestUtils.setField(category, "name", name);
        ReflectionTestUtils.setField(category, "categoryType", type);
        ReflectionTestUtils.setField(category, "color", "#16A34A");
        return category;
    }
}
