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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A schedule that produces INCOME or EXPENSE transactions (rent, salary, subscriptions).
 *
 * <p>Lifecycle and rules:
 * <ul>
 *   <li>{@link #generateNextOccurrence()} creates the transaction for {@code nextOccurrence} and
 *       advances the schedule in the same unit of work. Together with the database's unique
 *       (rule, occurrence date) index, this makes generation exactly-once.</li>
 *   <li>Edits (amount, account, category, description, end date) apply to future occurrences only.
 *       Frequency and start date are fixed.</li>
 *   <li>If the account is closed or the category archived, generation fails and the rule is
 *       paused with a reason. Resuming skips the occurrences missed while paused.</li>
 *   <li>Ending is permanent; generated history stays.</li>
 * </ul>
 */
@Entity
@Table(name = "recurring_transactions")
public class RecurringTransaction extends AuditableEntity {

    public static final int MAX_DESCRIPTION_LENGTH = 255;
    /** How far back a new rule may start; older history should be entered as ordinary transactions. */
    public static final int MAX_BACKDATE_DAYS = 366;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private FinancialAccount account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 10, updatable = false)
    private TransactionType type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "description", nullable = false, length = MAX_DESCRIPTION_LENGTH)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 12, updatable = false)
    private Frequency frequency;

    @Column(name = "start_date", nullable = false, updatable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "anchor_day", nullable = false, updatable = false)
    private int anchorDay;

    @Column(name = "next_occurrence")
    private LocalDate nextOccurrence;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private RecurringStatus status;

    @Column(name = "pause_reason", length = 255)
    private String pauseReason;

    @Column(name = "remind_days_before")
    private Integer remindDaysBefore;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** Required by JPA only. */
    protected RecurringTransaction() {
    }

    public RecurringTransaction(FinancialAccount account, Category category, TransactionType type, BigDecimal amount,
                                String description, Frequency frequency, LocalDate startDate, LocalDate endDate,
                                Integer remindDaysBefore, LocalDate today) {
        this.type = Objects.requireNonNull(type, "type");
        if (type.isTransfer()) {
            throw new BusinessRuleException("Recurring transfers are not supported; schedule income or expenses.");
        }
        this.frequency = Objects.requireNonNull(frequency, "frequency");
        this.startDate = Objects.requireNonNull(startDate, "startDate");
        if (startDate.isBefore(today.minusDays(MAX_BACKDATE_DAYS))) {
            throw new BusinessRuleException("A recurring transaction can start at most one year in the past.");
        }
        this.anchorDay = startDate.getDayOfMonth();
        applyChanges(account, category, amount, description, endDate, remindDaysBefore);
        this.nextOccurrence = startDate;
        this.status = RecurringStatus.ACTIVE;
        endIfPastEndDate();
    }

    /** Applies to future occurrences only; already generated transactions are untouched. */
    public void update(FinancialAccount newAccount, Category newCategory, BigDecimal newAmount, String newDescription,
                       LocalDate newEndDate, Integer newRemindDaysBefore) {
        if (status == RecurringStatus.ENDED) {
            throw new BusinessRuleException("This recurring transaction has ended and cannot be changed.");
        }
        applyChanges(newAccount, newCategory, newAmount, newDescription, newEndDate, newRemindDaysBefore);
        endIfPastEndDate();
    }

    private void applyChanges(FinancialAccount newAccount, Category newCategory, BigDecimal newAmount,
                              String newDescription, LocalDate newEndDate, Integer newRemindDaysBefore) {
        Objects.requireNonNull(newAccount, "account");
        Objects.requireNonNull(newCategory, "category");
        if (account != null && !Objects.equals(account.ownerId(), newAccount.ownerId())) {
            throw new BusinessRuleException("A recurring transaction cannot move to another user's account.");
        }
        newAccount.requireOpenForActivity();
        if (!newCategory.isVisibleTo(newAccount.ownerId())) {
            throw new BusinessRuleException("The category does not belong to the account owner.");
        }
        newCategory.requireUsableFor(type);
        Money money = Money.of(Objects.requireNonNull(newAmount, "amount"), newAccount.getCurrency());
        if (!money.isPositive()) {
            throw new BusinessRuleException("Amount must be greater than zero.");
        }
        if (newDescription == null || newDescription.isBlank()) {
            throw new BusinessRuleException("Give the recurring transaction a description, e.g. 'Rent'.");
        }
        String trimmed = newDescription.strip();
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BusinessRuleException("Description must be at most %d characters.".formatted(MAX_DESCRIPTION_LENGTH));
        }
        if (newEndDate != null && newEndDate.isBefore(startDate)) {
            throw new BusinessRuleException("End date cannot be before the start date.");
        }
        if (newRemindDaysBefore != null && (newRemindDaysBefore < 0 || newRemindDaysBefore > 30)) {
            throw new BusinessRuleException("Reminders can be sent 0 to 30 days before each occurrence.");
        }
        this.account = newAccount;
        this.category = newCategory;
        this.amount = money.amount();
        this.description = trimmed;
        this.endDate = newEndDate;
        this.remindDaysBefore = newRemindDaysBefore;
    }

    public boolean isDue(LocalDate today) {
        return status == RecurringStatus.ACTIVE && nextOccurrence != null && !nextOccurrence.isAfter(today);
    }

    /**
     * Creates the transaction for the current {@code nextOccurrence} and advances the schedule.
     * The account and category are re-checked; if either can no longer be used, a
     * {@link BusinessRuleException} is thrown and nothing changes (the caller pauses the rule).
     */
    public Transaction generateNextOccurrence() {
        if (status != RecurringStatus.ACTIVE || nextOccurrence == null) {
            throw new IllegalStateException("Rule %d has nothing to generate".formatted(id));
        }
        Transaction occurrence = Transaction.recurringOccurrence(this, account, category, type, amount, description, nextOccurrence);
        nextOccurrence = frequency.next(nextOccurrence, anchorDay);
        endIfPastEndDate();
        return occurrence;
    }

    public void pause(String reason) {
        if (status == RecurringStatus.ENDED) {
            throw new BusinessRuleException("This recurring transaction has ended.");
        }
        status = RecurringStatus.PAUSED;
        pauseReason = reason;
    }

    /** Resumes from the first scheduled date on or after {@code today}; missed occurrences are skipped. */
    public void resume(LocalDate today) {
        if (status == RecurringStatus.ENDED) {
            throw new BusinessRuleException("This recurring transaction has ended and cannot be resumed.");
        }
        account.requireOpenForActivity();
        category.requireUsableFor(type);
        LocalDate next = nextOccurrence;
        while (next != null && next.isBefore(today)) {
            next = frequency.next(next, anchorDay);
        }
        nextOccurrence = next;
        status = RecurringStatus.ACTIVE;
        pauseReason = null;
        endIfPastEndDate();
    }

    public void end() {
        status = RecurringStatus.ENDED;
        nextOccurrence = null;
        pauseReason = null;
    }

    private void endIfPastEndDate() {
        if (nextOccurrence != null && endDate != null && nextOccurrence.isAfter(endDate)) {
            end();
        }
    }

    /** The next {@code count} scheduled dates (for previews and reminders). */
    public List<LocalDate> upcoming(int count) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate next = status == RecurringStatus.ENDED ? null : nextOccurrence;
        while (next != null && dates.size() < count && (endDate == null || !next.isAfter(endDate))) {
            dates.add(next);
            next = frequency.next(next, anchorDay);
        }
        return dates;
    }

    public boolean isOwnedBy(long userId) {
        return account.isOwnedBy(userId);
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

    public Money amount() {
        return Money.of(amount, account.getCurrency());
    }

    public String getDescription() {
        return description;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LocalDate getNextOccurrence() {
        return nextOccurrence;
    }

    public RecurringStatus getStatus() {
        return status;
    }

    public String getPauseReason() {
        return pauseReason;
    }

    public Integer getRemindDaysBefore() {
        return remindDaysBefore;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RecurringTransaction that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(RecurringTransaction.class);
    }

    @Override
    public String toString() {
        return "RecurringTransaction{id=%d, frequency=%s, status=%s, next=%s}".formatted(id, frequency, status, nextOccurrence);
    }
}
