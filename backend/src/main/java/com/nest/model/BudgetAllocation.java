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
import java.util.Objects;

/** A spending limit for one expense category within a monthly budget. Created and validated only by its budget. */
@Entity
@Table(name = "budget_allocations")
public class BudgetAllocation extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false, updatable = false)
    private MonthlyBudget budget;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "limit_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal limitAmount;

    /** Required by JPA only. */
    protected BudgetAllocation() {
    }

    BudgetAllocation(MonthlyBudget budget, Category category, Money limit) {
        this.budget = budget;
        this.category = category;
        this.limitAmount = limit.amount();
    }

    void changeLimit(Money limit) {
        this.limitAmount = limit.amount();
    }

    public Long getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public Money limit() {
        return Money.of(limitAmount, budget.getCurrency());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BudgetAllocation that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(BudgetAllocation.class);
    }
}
