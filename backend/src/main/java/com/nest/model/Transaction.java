package com.nest.model;

import com.nest.exception.BusinessRuleException;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * One movement of money on one account.
 *
 * <p>Invariants, checked on creation and on every revision:
 * <ul>
 *   <li>The amount is strictly positive and fits the account currency's decimal places. The direction comes from the type.</li>
 *   <li>The account is open (closed accounts keep their history frozen).</li>
 *   <li>The category is visible to the account's owner, not archived, and matches the type (EXPENSE ↔ EXPENSE).</li>
 * </ul>
 * The currency is not stored: it is always the account's currency.
 */
@Entity
@Table(name = "transactions")
public class Transaction extends AuditableEntity {

    public static final int MAX_DESCRIPTION_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private FinancialAccount account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "description", length = MAX_DESCRIPTION_LENGTH)
    private String description;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** Required by JPA only. */
    protected Transaction() {
    }

    /** Records an INCOME or EXPENSE entry. Transfer legs are created by the transfer itself (increment 5). */
    public Transaction(FinancialAccount account, Category category, TransactionType type,
                       BigDecimal amount, String description, LocalDate transactionDate) {
        apply(account, category, type, amount, description, transactionDate);
    }

    /**
     * Replaces every editable field and re-checks all invariants. The entry may move to another
     * open account of the same owner. Its current account must also be open, because a closed
     * account's history is frozen.
     */
    public void revise(FinancialAccount newAccount, Category newCategory, TransactionType newType,
                       BigDecimal newAmount, String newDescription, LocalDate newDate) {
        account.requireOpenForActivity();
        if (!Objects.equals(account.ownerId(), newAccount.ownerId())) {
            throw new BusinessRuleException("A transaction cannot be moved to another user's account.");
        }
        apply(newAccount, newCategory, newType, newAmount, newDescription, newDate);
    }

    /** Checks that this entry may be deleted. Only entries on open accounts can be. */
    public void requireRemovable() {
        account.requireOpenForActivity();
    }

    private void apply(FinancialAccount newAccount, Category newCategory, TransactionType newType,
                       BigDecimal newAmount, String newDescription, LocalDate newDate) {
        Objects.requireNonNull(newAccount, "account");
        Objects.requireNonNull(newType, "type");
        Objects.requireNonNull(newDate, "transactionDate");
        if (newType.isTransfer()) {
            throw new BusinessRuleException("Transfers must be recorded through /api/transfers.");
        }
        newAccount.requireOpenForActivity();
        requireCategoryFor(newAccount, newCategory, newType);
        Money money = Money.of(Objects.requireNonNull(newAmount, "amount"), newAccount.getCurrency());
        if (!money.isPositive()) {
            throw new BusinessRuleException("Amount must be greater than zero; use the type for direction.");
        }

        this.account = newAccount;
        this.category = newCategory;
        this.type = newType;
        this.amount = money.amount();
        this.description = normalizeDescription(newDescription);
        this.transactionDate = newDate;
    }

    private static void requireCategoryFor(FinancialAccount account, Category category, TransactionType type) {
        if (category == null) {
            throw new BusinessRuleException("A %s transaction requires a category.".formatted(type));
        }
        if (!category.isVisibleTo(account.ownerId())) {
            throw new BusinessRuleException("The category does not belong to the account owner.");
        }
        category.requireUsableFor(type);
    }

    private static String normalizeDescription(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        String trimmed = candidate.strip();
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BusinessRuleException("Description must be at most %d characters.".formatted(MAX_DESCRIPTION_LENGTH));
        }
        return trimmed;
    }

    /** Always the account's currency. */
    public String currency() {
        return account.getCurrency();
    }

    public Money money() {
        return Money.of(amount, currency());
    }

    /** Effect on the account balance: +amount for inflows, −amount for outflows. */
    public BigDecimal signedAmount() {
        return type.balanceSign() > 0 ? money().amount() : money().amount().negate();
    }

    public Long getId() {
        return id;
    }

    public FinancialAccount getAccount() {
        return account;
    }

    public Category getCategory() {
        return category;
    }

    public TransactionType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Transaction that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(Transaction.class);
    }

    @Override
    public String toString() {
        return "Transaction{id=%d, type=%s, date=%s}".formatted(id, type, transactionDate);
    }
}
