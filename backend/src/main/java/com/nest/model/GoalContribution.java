package com.nest.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Money earmarked for a goal on a date. A tracked allocation: it does not move money between
 * accounts. Created only by {@link SavingsGoal#contribute}.
 */
@Entity
@Table(name = "goal_contributions")
public class GoalContribution extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_id", nullable = false, updatable = false)
    private SavingsGoal goal;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal amount;

    @Column(name = "contribution_date", nullable = false, updatable = false)
    private LocalDate contributionDate;

    @Column(name = "note", length = 255, updatable = false)
    private String note;

    @Column(name = "idempotency_key", length = 64, updatable = false)
    private String idempotencyKey;

    /** Required by JPA only. */
    protected GoalContribution() {
    }

    GoalContribution(SavingsGoal goal, Money amount, LocalDate contributionDate, String note, String idempotencyKey) {
        this.goal = goal;
        this.amount = amount.amount();
        this.contributionDate = contributionDate;
        this.note = note;
        this.idempotencyKey = idempotencyKey;
    }

    public Long getId() {
        return id;
    }

    public Money amount() {
        return Money.of(amount, goal.getCurrency());
    }

    public LocalDate getContributionDate() {
        return contributionDate;
    }

    public String getNote() {
        return note;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GoalContribution that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(GoalContribution.class);
    }
}
