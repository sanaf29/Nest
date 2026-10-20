package com.nest.repository;

import com.nest.model.Transaction;
import com.nest.model.TransactionType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Composable transaction filters (Specification pattern). Each criterion is independent and
 * returns {@code null} when unused, which Spring Data ignores. A new filter is a new method here,
 * with no change to the repository or query-building code (open/closed).
 */
public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    /** Mandatory scope: only the user's own transactions. */
    public static Specification<Transaction> ownedBy(long userId) {
        return (root, query, cb) -> cb.equal(root.get("account").get("user").get("id"), userId);
    }

    public static Specification<Transaction> inAccount(Long accountId) {
        return accountId == null ? null : (root, query, cb) -> cb.equal(root.get("account").get("id"), accountId);
    }

    public static Specification<Transaction> inCurrency(String currency) {
        return currency == null ? null : (root, query, cb) -> cb.equal(root.get("account").get("currency"), currency);
    }

    public static Specification<Transaction> inCategory(Long categoryId) {
        return categoryId == null ? null : (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Transaction> ofType(TransactionType type) {
        return type == null ? null : (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<Transaction> onOrAfter(LocalDate from) {
        return from == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("transactionDate"), from);
    }

    public static Specification<Transaction> onOrBefore(LocalDate to) {
        return to == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("transactionDate"), to);
    }

    public static Specification<Transaction> amountAtLeast(BigDecimal min) {
        return min == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("amount"), min);
    }

    public static Specification<Transaction> amountAtMost(BigDecimal max) {
        return max == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("amount"), max);
    }

    /** Case-insensitive substring match on the description. LIKE wildcards in the input are escaped. */
    public static Specification<Transaction> descriptionContains(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String pattern = "%" + escapeLike(text.strip().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("description")), pattern, '\\');
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_").replace("[", "\\[");
    }
}
