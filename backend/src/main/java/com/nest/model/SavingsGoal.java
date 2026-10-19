package com.nest.model;

import com.nest.exception.BusinessRuleException;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Something the user is saving toward, such as "Emergency fund: 5,000 CAD by June".
 *
 * <p>Progress comes only from contributions; the saved amount is derived, never set. The status
 * follows the numbers: {@link GoalStatus#ACHIEVED} once contributions reach the target, back to
 * {@link GoalStatus#ACTIVE} if the target is raised or a contribution is removed.
 */
@Entity
@Table(name = "savings_goals")
public class SavingsGoal extends AuditableEntity {

    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_NOTE_LENGTH = 255;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "name", nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(name = "currency", nullable = false, length = 3, columnDefinition = "char(3)", updatable = false)
    private String currency;

    @Column(name = "target_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal targetAmount;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private GoalStatus status;

    @Column(name = "achieved_at")
    private Instant achievedAt;

    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("contributionDate DESC, id DESC")
    private List<GoalContribution> contributions = new ArrayList<>();

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** Required by JPA only. */
    protected SavingsGoal() {
    }

    /**
     * @param targetDate optional deadline; must not be before {@code today}
     * @param today      the current date, passed in so the rule is testable
     */
    public SavingsGoal(User owner, String name, String currency, BigDecimal targetAmount, LocalDate targetDate, LocalDate today) {
        this.user = Objects.requireNonNull(owner, "owner");
        this.currency = Currencies.requireSupported(currency);
        if (targetDate != null && targetDate.isBefore(today)) {
            throw new BusinessRuleException("Target date cannot be in the past.");
        }
        this.name = validName(name);
        this.targetAmount = validTarget(targetAmount);
        this.targetDate = targetDate;
        this.status = GoalStatus.ACTIVE;
    }

    /** Changes name, target, and deadline, then re-evaluates the status. */
    public void update(String newName, BigDecimal newTarget, LocalDate newTargetDate, Instant now) {
        String name = validName(newName);
        BigDecimal target = validTarget(newTarget);
        this.name = name;
        this.targetAmount = target;
        this.targetDate = newTargetDate;
        refreshStatus(now);
    }

    /**
     * Records a contribution.
     *
     * @param idempotencyKey optional client-chosen key; the caller checks {@link #contributionWithKey}
     *                       first so a retried request is not counted twice
     * @throws BusinessRuleException if the amount is not positive, the date is in the future, or the key is reused
     */
    public GoalContribution contribute(BigDecimal amount, LocalDate date, String note, String idempotencyKey,
                                       LocalDate today, Instant now) {
        Money money = Money.of(Objects.requireNonNull(amount, "amount"), currency);
        if (!money.isPositive()) {
            throw new BusinessRuleException("Contribution must be greater than zero.");
        }
        Objects.requireNonNull(date, "date");
        if (date.isAfter(today)) {
            throw new BusinessRuleException("Contribution date cannot be in the future.");
        }
        String trimmedNote = note == null || note.isBlank() ? null : note.strip();
        if (trimmedNote != null && trimmedNote.length() > MAX_NOTE_LENGTH) {
            throw new BusinessRuleException("Note must be at most %d characters.".formatted(MAX_NOTE_LENGTH));
        }
        if (idempotencyKey != null && contributionWithKey(idempotencyKey).isPresent()) {
            throw new BusinessRuleException("A contribution with this idempotency key already exists.");
        }
        GoalContribution contribution = new GoalContribution(this, money, date, trimmedNote, idempotencyKey);
        contributions.add(contribution);
        refreshStatus(now);
        return contribution;
    }

    public Optional<GoalContribution> contributionWithKey(String idempotencyKey) {
        return contributions.stream().filter(c -> idempotencyKey.equals(c.getIdempotencyKey())).findFirst();
    }

    /** @return false if no contribution with that id belongs to this goal */
    public boolean removeContribution(long contributionId, Instant now) {
        boolean removed = contributions.removeIf(c -> c.getId() != null && c.getId() == contributionId);
        if (removed) {
            refreshStatus(now);
        }
        return removed;
    }

    private void refreshStatus(Instant now) {
        boolean reached = savedAmount().amount().compareTo(targetAmount) >= 0;
        if (reached && status == GoalStatus.ACTIVE) {
            status = GoalStatus.ACHIEVED;
            achievedAt = now;
        } else if (!reached && status == GoalStatus.ACHIEVED) {
            status = GoalStatus.ACTIVE;
            achievedAt = null;
        }
    }

    public Money target() {
        return Money.of(targetAmount, currency);
    }

    public Money savedAmount() {
        return contributions.stream().map(GoalContribution::amount).reduce(Money.zero(currency), Money::plus);
    }

    /** What is still needed; never negative. */
    public Money remainingAmount() {
        Money remaining = target().minus(savedAmount());
        return remaining.isNegative() ? Money.zero(currency) : remaining;
    }

    /** Saved ÷ target × 100, HALF_EVEN to 2 decimals, capped at 100 for display. */
    public BigDecimal progressPercent() {
        BigDecimal percent = savedAmount().amount().multiply(HUNDRED).divide(targetAmount, 2, RoundingMode.HALF_EVEN);
        return percent.min(HUNDRED.setScale(2));
    }

    private static String validName(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            throw new BusinessRuleException("Goal name must not be blank.");
        }
        String trimmed = candidate.strip();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new BusinessRuleException("Goal name must be at most %d characters.".formatted(MAX_NAME_LENGTH));
        }
        return trimmed;
    }

    private BigDecimal validTarget(BigDecimal candidate) {
        Money target = Money.of(Objects.requireNonNull(candidate, "targetAmount"), currency);
        if (!target.isPositive()) {
            throw new BusinessRuleException("Target amount must be greater than zero.");
        }
        return target.amount();
    }

    public boolean isOwnedBy(long userId) {
        return user.getId() != null && user.getId() == userId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public GoalStatus getStatus() {
        return status;
    }

    public Instant getAchievedAt() {
        return achievedAt;
    }

    public List<GoalContribution> getContributions() {
        return Collections.unmodifiableList(contributions);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SavingsGoal that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(SavingsGoal.class);
    }

    @Override
    public String toString() {
        return "SavingsGoal{id=%d, status=%s}".formatted(id, status);
    }
}
