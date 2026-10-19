package com.nest.repository;

import com.nest.model.Category;
import com.nest.model.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * A user can see the default categories and their own. Every query here encodes that rule,
 * so another user's category is never returned.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("""
            select c from Category c
            where c.id = :id and (c.user is null or c.user.id = :userId)
            """)
    Optional<Category> findVisible(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * @param type            restrict to one type, or {@code null} for both
     * @param includeArchived whether archived user categories are included
     */
    @Query("""
            select c from Category c
            where (c.user is null or c.user.id = :userId)
              and (:type is null or c.categoryType = :type)
              and (:includeArchived = true or c.archived = false)
            order by c.categoryType, c.name
            """)
    List<Category> findVisible(@Param("userId") Long userId,
                               @Param("type") CategoryType type,
                               @Param("includeArchived") boolean includeArchived);

    /**
     * True if a non-archived category with this name (case-insensitive) and type is visible to the
     * user, either a default or one of their own, other than {@code excludedId}.
     */
    @Query("""
            select count(c) > 0 from Category c
            where (c.user is null or c.user.id = :userId)
              and c.archived = false
              and c.categoryType = :type
              and lower(c.name) = lower(:name)
              and (:excludedId is null or c.id <> :excludedId)
            """)
    boolean existsActiveVisibleName(@Param("userId") Long userId,
                                    @Param("name") String name,
                                    @Param("type") CategoryType type,
                                    @Param("excludedId") Long excludedId);
}
