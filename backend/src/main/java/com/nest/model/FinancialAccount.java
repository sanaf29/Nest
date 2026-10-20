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
import java.util.Map;
import java.util.Objects;

/**
 * A place where the user holds or owes money: a checking, savings, cash, or credit card account.
 *
 * <p>Invariants:
 * <ul>
 *   <li>The currency and type are fixed at creation.</li>
 *   <li>The opening balance respects the type's sign rule and the currency's decimal places.</li>
 *   <li>There is no settable balance. The balance is always derived ({@link #calculateBalance}).</li>
 *   <li>A closed (inactive) account cannot be changed. Its history stays intact.</li>
 * </ul>
 */
@Entity
@Table(name = "financial_accounts")
public class FinancialAccount extends AuditableEntity {

    public static final int MAX_NAME_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "name", nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20, updatable = false)
    private AccountType accountType;

    @Column(name = "currency", nullable = false, length = 3, columnDefinition = "char(3)", updatable = false)
    private String currency;

    @Column(name = "opening_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal openingBalance;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** Required by JPA only. */
    protected FinancialAccount() {
    }

    public FinancialAccount(User owner, String name, AccountType accountType, String currency,
                            BigDecimal openingBalance) {
        this.user = Objects.requireNonNull(owner, "owner");
        this.accountType = Objects.requireNonNull(accountType, "accountType");
        this.currency = Currencies.requireSupported(currency);
        this.name = validName(name);
        this.openingBalance = validOpeningBalance(openingBalance);
        this.active = true;
    }

    public void rename(String newName) {
        requireActive();
        this.name = validName(newName);
    }

    /**
     * Corrects the starting point of the ledger. This changes the derived balance by the same
     * difference, which is how a user fixes a wrong starting figure. It is not a way to set the
     * balance directly.
     */
    public void correctOpeningBalance(BigDecimal newOpeningBalance) {
        requireActive();
        this.openingBalance = validOpeningBalance(newOpeningBalance);
    }

    /** Closes the account. Calling it again has no effect, which makes DELETE idempotent. */
    public void deactivate() {
        this.active = false;
    }

    public boolean isOwnedBy(long userId) {
        return user.getId() != null && user.getId() == userId;
    }

    public User getUser() {
        return user;
    }

    /** Owner's id. Reading the id of a lazy proxy does not load the user. */
    public Long ownerId() {
        return user.getId();
    }

    /**
     * Guards changes to the account's ledger: closed accounts keep their history exactly as it was.
     *
     * @throws BusinessRuleException if the account is closed
     */
    public void requireOpenForActivity() {
        if (!active) {
            throw new BusinessRuleException("Account '%s' is closed; its transactions cannot be added, changed, or removed."
                    .formatted(name));
        }
    }

    /**
     * Balance = opening balance + Σ (sign(type) × total(type)).
     *
     * @param totalsByType sum of entry amounts per transaction type for this account; missing types count as zero
     */
    public Money calculateBalance(Map<TransactionType, BigDecimal> totalsByType) {
        Money balance = openingBalance();
        for (Map.Entry<TransactionType, BigDecimal> total : totalsByType.entrySet()) {
            Money amount = Money.of(total.getValue(), currency);
            balance = total.getKey().balanceSign() > 0 ? balance.plus(amount) : balance.minus(amount);
        }
        return balance;
    }

    public Money openingBalance() {
        return Money.of(openingBalance, currency);
    }

    private void requireActive() {
        if (!active) {
            throw new BusinessRuleException("Account '%s' is closed and cannot be changed.".formatted(name));
        }
    }

    private static String validName(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            throw new BusinessRuleException("Account name must not be blank.");
        }
        String trimmed = candidate.strip();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new BusinessRuleException("Account name must be at most %d characters.".formatted(MAX_NAME_LENGTH));
        }
        return trimmed;
    }

    private BigDecimal validOpeningBalance(BigDecimal candidate) {
        Money opening = Money.of(Objects.requireNonNull(candidate, "openingBalance"), currency);
        if (opening.isNegative() && !accountType.allowsNegativeOpeningBalance()) {
            throw new BusinessRuleException("A %s account cannot have a negative opening balance."
                    .formatted(accountType.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ')));
        }
        return opening.amount();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public String getCurrency() {
        return currency;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FinancialAccount that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(FinancialAccount.class);
    }

    @Override
    public String toString() {
        return "FinancialAccount{id=%d, type=%s, currency=%s, active=%s}".formatted(id, accountType, currency, active);
    }
}
