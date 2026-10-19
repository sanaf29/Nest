package com.nest.model;

import com.nest.exception.BusinessRuleException;
import com.nest.exception.ForbiddenOperationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Classifies income or expenses.
 *
 * <p>There are two kinds, told apart by ownership rather than by a flag or a subclass:
 * <ul>
 *   <li><b>Default</b> ({@code user == null}): seeded by migration, visible to everyone, immutable.</li>
 *   <li><b>User-defined</b>: visible to and editable by its owner only.</li>
 * </ul>
 * The type is fixed at creation, so changing it can never reclassify past transactions.
 * Archiving hides a category from new use while existing transactions keep it.
 */
@Entity
@Table(name = "categories")
public class Category extends AuditableEntity {

    public static final int MAX_NAME_LENGTH = 50;
    public static final String DEFAULT_COLOR = "#6B7280";
    private static final Pattern HEX_COLOR = Pattern.compile("#[0-9A-Fa-f]{6}");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null for system default categories. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", updatable = false)
    private User user;

    @Column(name = "name", nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_type", nullable = false, length = 10, updatable = false)
    private CategoryType categoryType;

    @Column(name = "color", nullable = false, length = 7, columnDefinition = "char(7)")
    private String color;

    @Column(name = "archived", nullable = false)
    private boolean archived;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** Required by JPA only. */
    protected Category() {
    }

    /**
     * Creates a category owned by {@code owner}. Default categories are created only by database
     * migration, so this is the only public way to create one.
     *
     * @param color {@code #RRGGBB}; {@code null} uses {@link #DEFAULT_COLOR}
     */
    public Category(User owner, String name, CategoryType categoryType, String color) {
        this.user = Objects.requireNonNull(owner, "owner");
        this.categoryType = Objects.requireNonNull(categoryType, "categoryType");
        this.name = validName(name);
        this.color = validColor(color);
        this.archived = false;
    }

    public boolean isDefault() {
        return user == null;
    }

    public boolean isOwnedBy(long userId) {
        return user != null && user.getId() != null && user.getId() == userId;
    }

    /** Defaults are visible to everyone; user-defined categories only to their owner. */
    public boolean isVisibleTo(long userId) {
        return isDefault() || isOwnedBy(userId);
    }

    /** Renames and recolours. Defaults are read-only, and archived categories must stay as they were. */
    public void update(String newName, String newColor) {
        requireUserDefined("edited");
        if (archived) {
            throw new BusinessRuleException("Category '%s' is archived and cannot be changed.".formatted(name));
        }
        this.name = validName(newName);
        this.color = validColor(newColor);
    }

    /** Hides the category from new use. Calling it again has no effect, which makes DELETE idempotent. */
    public void archive() {
        requireUserDefined("deleted");
        this.archived = true;
    }

    /** True if this category can classify an entry of the given type (e.g. EXPENSE ↔ EXPENSE). */
    public boolean supports(TransactionType transactionType) {
        return transactionType.requiredCategoryType().map(categoryType::equals).orElse(false);
    }

    /**
     * Guards the use of this category on a new or edited transaction.
     *
     * @throws BusinessRuleException if it is archived or of the wrong type
     */
    public void requireUsableFor(TransactionType transactionType) {
        if (archived) {
            throw new BusinessRuleException("Category '%s' is archived and cannot be used.".formatted(name));
        }
        if (!supports(transactionType)) {
            throw new BusinessRuleException("Category '%s' is a %s category and cannot classify a %s transaction."
                    .formatted(name, categoryType, transactionType));
        }
    }

    private void requireUserDefined(String action) {
        if (isDefault()) {
            throw new ForbiddenOperationException("Default category '%s' cannot be %s.".formatted(name, action));
        }
    }

    private static String validName(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            throw new BusinessRuleException("Category name must not be blank.");
        }
        String trimmed = candidate.strip();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new BusinessRuleException("Category name must be at most %d characters.".formatted(MAX_NAME_LENGTH));
        }
        return trimmed;
    }

    private static String validColor(String candidate) {
        if (candidate == null) {
            return DEFAULT_COLOR;
        }
        if (!HEX_COLOR.matcher(candidate).matches()) {
            throw new BusinessRuleException("Colour must be a hex value like #1A2B3C.");
        }
        return candidate.toUpperCase(Locale.ROOT);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public CategoryType getCategoryType() {
        return categoryType;
    }

    public String getColor() {
        return color;
    }

    public boolean isArchived() {
        return archived;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Category that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(Category.class);
    }

    @Override
    public String toString() {
        return "Category{id=%d, name='%s', type=%s, default=%s, archived=%s}"
                .formatted(id, name, categoryType, isDefault(), archived);
    }
}
