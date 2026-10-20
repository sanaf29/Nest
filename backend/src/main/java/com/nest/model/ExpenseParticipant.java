package com.nest.model;

import com.nest.exception.BusinessRuleException;
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
import java.time.Instant;
import java.util.Objects;

/** One person in a shared expense and their share. Created and changed only through {@link SharedExpense}. */
@Entity
@Table(name = "expense_participants")
public class ExpenseParticipant extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shared_expense_id", nullable = false, updatable = false)
    private SharedExpense expense;

    @Column(name = "position", nullable = false, updatable = false)
    private int position;

    @Column(name = "name", nullable = false, length = 100, updatable = false)
    private String name;

    @Column(name = "email", length = 254, updatable = false)
    private String email;

    @Column(name = "is_me", nullable = false, updatable = false)
    private boolean me;

    @Column(name = "is_payer", nullable = false, updatable = false)
    private boolean payer;

    @Column(name = "share_amount", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal shareAmount;

    @Column(name = "settled_at")
    private Instant settledAt;

    /** Required by JPA only. */
    protected ExpenseParticipant() {
    }

    ExpenseParticipant(SharedExpense expense, int position, String name, String email, boolean me, boolean payer, Money share) {
        this.expense = expense;
        this.position = position;
        this.name = name;
        this.email = email;
        this.me = me;
        this.payer = payer;
        this.shareAmount = share.amount();
    }

    /** Whether this participant owes the payer anything (non-payer with a non-zero share). */
    public boolean owesPayer() {
        return !payer && shareAmount.signum() > 0;
    }

    void settle(Instant now) {
        if (!owesPayer()) {
            throw new BusinessRuleException("%s doesn't owe anything on this expense.".formatted(name));
        }
        if (settledAt == null) {
            settledAt = now;
        }
    }

    void unsettle() {
        settledAt = null;
    }

    public boolean isSettled() {
        return settledAt != null;
    }

    public Long getId() {
        return id;
    }

    public int getPosition() {
        return position;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public boolean isMe() {
        return me;
    }

    public boolean isPayer() {
        return payer;
    }

    public Money share() {
        return Money.of(shareAmount, expense.getCurrency());
    }

    public Instant getSettledAt() {
        return settledAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ExpenseParticipant that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ExpenseParticipant.class);
    }
}
