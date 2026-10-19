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
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A user's plan for one calendar month in one currency: planned income, a savings strategy, and
 * spending limits per expense category.
 *
 * <p>Invariants:
 * <ul>
 *   <li>The period and currency are fixed; one budget per user, month, and currency (database constraint).</li>
 *   <li>The savings target never exceeds planned income.</li>
 *   <li>Allocations use distinct, visible, unarchived EXPENSE categories with positive limits whose
 *       total does not exceed the spendable amount (income − savings).</li>
 * </ul>
 * Every change is validated before any field is modified, so a rejected change leaves the budget intact.
 */
@Entity
@Table(name = "monthly_budgets")
public class MonthlyBudget extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "budget_year", nullable = false, updatable = false)
    private int year;

    @Column(name = "budget_month", nullable = false, updatable = false)
    private int month;

    @Column(name = "currency", nullable = false, length = 3, columnDefinition = "char(3)", updatable = false)
    private String currency;

    @Column(name = "planned_income", nullable = false, precision = 19, scale = 4)
    private BigDecimal plannedIncome;

    @Enumerated(EnumType.STRING)
    @Column(name = "savings_strategy", nullable = false, length = 20)
    private SavingsStrategyType savingsStrategy;

    @Column(name = "savings_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal savingsValue;

    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<BudgetAllocation> allocations = new ArrayList<>();

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** Required by JPA only. */
    protected MonthlyBudget() {
    }

    public MonthlyBudget(User owner, YearMonth period, String currency, BigDecimal plannedIncome,
                         SavingsStrategyType savingsStrategy, BigDecimal savingsValue) {
        this.user = Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(period, "period");
        if (period.getYear() < 2000 || period.getYear() > 2100) {
            throw new BusinessRuleException("Budget year must be between 2000 and 2100.");
        }
        this.year = period.getYear();
        this.month = period.getMonthValue();
        this.currency = Currencies.requireSupported(currency);
        revisePlan(plannedIncome, savingsStrategy, savingsValue);
    }

    /** One allocation request: a category and its limit. */
    public record AllocationSpec(Category category, BigDecimal limit) {
    }

    /**
     * Sets income and savings. Existing allocations must still fit the new spendable amount.
     * Used on creation; {@link #revise} changes plan and allocations together.
     */
    public void revisePlan(BigDecimal newPlannedIncome, SavingsStrategyType newStrategy, BigDecimal newSavingsValue) {
        Money spendable = spendableFor(newPlannedIncome, newStrategy, newSavingsValue);
        requireWithinSpendable(currentAllocatedTotal(), spendable);
        applyPlan(newPlannedIncome, newStrategy, newSavingsValue);
    }

    /**
     * Replaces the plan and every allocation in one step (PUT semantics). The new allocations are
     * validated against the new plan before anything changes. Rows for categories that are kept
     * are updated in place, which avoids delete-then-insert collisions on the
     * (budget, category) unique key.
     */
    public void revise(BigDecimal newPlannedIncome, SavingsStrategyType newStrategy, BigDecimal newSavingsValue,
                       List<AllocationSpec> specs) {
        Money spendable = spendableFor(newPlannedIncome, newStrategy, newSavingsValue);
        Map<Long, Money> validated = validateAllocations(specs, spendable);
        applyPlan(newPlannedIncome, newStrategy, newSavingsValue);
        applyAllocations(specs, validated);
    }

    /** Replaces all allocations, keeping the current plan. */
    public void replaceAllocations(List<AllocationSpec> specs) {
        applyAllocations(specs, validateAllocations(specs, spendable()));
    }

    private Money spendableFor(BigDecimal newPlannedIncome, SavingsStrategyType newStrategy, BigDecimal newSavingsValue) {
        Money income = Money.of(Objects.requireNonNull(newPlannedIncome, "plannedIncome"), currency);
        if (income.isNegative()) {
            throw new BusinessRuleException("Planned income cannot be negative.");
        }
        Objects.requireNonNull(newStrategy, "savingsStrategy");
        Objects.requireNonNull(newSavingsValue, "savingsValue");
        return income.minus(newStrategy.create(newSavingsValue).savingsFor(income));
    }

    private void applyPlan(BigDecimal newPlannedIncome, SavingsStrategyType newStrategy, BigDecimal newSavingsValue) {
        this.plannedIncome = Money.of(newPlannedIncome, currency).amount();
        this.savingsStrategy = newStrategy;
        this.savingsValue = newSavingsValue;
    }

    private void applyAllocations(List<AllocationSpec> specs, Map<Long, Money> validated) {
        Map<Long, Category> categories = new HashMap<>();
        specs.forEach(spec -> categories.put(spec.category().getId(), spec.category()));

        allocations.removeIf(existing -> !validated.containsKey(existing.getCategory().getId()));
        Set<Long> kept = new HashSet<>();
        for (BudgetAllocation existing : allocations) {
            existing.changeLimit(validated.get(existing.getCategory().getId()));
            kept.add(existing.getCategory().getId());
        }
        validated.forEach((categoryId, limit) -> {
            if (!kept.contains(categoryId)) {
                allocations.add(new BudgetAllocation(this, categories.get(categoryId), limit));
            }
        });
    }

    private Map<Long, Money> validateAllocations(List<AllocationSpec> specs, Money spendable) {
        Map<Long, Money> limits = new HashMap<>();
        Money total = Money.zero(currency);
        for (AllocationSpec spec : specs) {
            Category category = Objects.requireNonNull(spec.category(), "category");
            if (!category.isVisibleTo(user.getId())) {
                throw new BusinessRuleException("Category '%s' is not available to this user.".formatted(category.getName()));
            }
            if (category.getCategoryType() != CategoryType.EXPENSE) {
                throw new BusinessRuleException("Only expense categories can have a spending limit; '%s' is %s."
                        .formatted(category.getName(), category.getCategoryType()));
            }
            Money limit = Money.of(Objects.requireNonNull(spec.limit(), "limit"), currency);
            if (!limit.isPositive()) {
                throw new BusinessRuleException("Limit for '%s' must be greater than zero.".formatted(category.getName()));
            }
            boolean alreadyAllocated = currentLimitFor(category.getId()) != null;
            if (category.isArchived() && !alreadyAllocated) {
                throw new BusinessRuleException("Category '%s' is archived.".formatted(category.getName()));
            }
            if (limits.put(category.getId(), limit) != null) {
                throw new BusinessRuleException("Category '%s' is allocated more than once.".formatted(category.getName()));
            }
            total = total.plus(limit);
        }
        requireWithinSpendable(total, spendable);
        return limits;
    }

    private Money currentLimitFor(Long categoryId) {
        return allocations.stream()
                .filter(a -> a.getCategory().getId().equals(categoryId))
                .map(BudgetAllocation::limit)
                .findFirst()
                .orElse(null);
    }

    private Money currentAllocatedTotal() {
        return allocations.stream().map(BudgetAllocation::limit).reduce(Money.zero(currency), Money::plus);
    }

    private static void requireWithinSpendable(Money allocated, Money spendable) {
        if (allocated.amount().compareTo(spendable.amount()) > 0) {
            throw new BusinessRuleException("Category limits total %s, but only %s is available after savings."
                    .formatted(allocated, spendable));
        }
    }

    /**
     * Compares the plan with what actually happened. Pure calculation, with no I/O.
     *
     * @param actuals income and expenses for this budget's month and currency
     */
    public BudgetSummary summarize(MonthlyActuals actuals) {
        if (!actuals.income().currency().equals(currency)) {
            throw new IllegalArgumentException("Actuals are in %s, budget is in %s".formatted(actuals.income().currency(), currency));
        }
        Money income = plannedIncome();
        Money savings = savingsTarget();
        Money spendable = income.minus(savings);
        Money expenses = actuals.totalExpenses();

        List<BudgetSummary.CategoryLine> lines = new ArrayList<>();
        Money budgetedSpending = Money.zero(currency);
        for (BudgetAllocation allocation : allocations) {
            Money limit = allocation.limit();
            Money spent = actuals.expensesIn(allocation.getCategory().getId());
            budgetedSpending = budgetedSpending.plus(spent);
            BigDecimal percent = BudgetSummary.percent(spent, limit);
            lines.add(new BudgetSummary.CategoryLine(allocation.getCategory(), limit, spent, limit.minus(spent),
                    percent, BudgetStatus.of(percent)));
        }

        return new BudgetSummary(
                income,
                savings,
                spendable,
                currentAllocatedTotal(),
                actuals.income(),
                expenses,
                actuals.netCashFlow(),
                spendable.minus(expenses),
                BudgetSummary.percent(expenses, spendable),
                BudgetSummary.percent(actuals.netCashFlow(), savings),
                expenses.minus(budgetedSpending),
                List.copyOf(lines));
    }

    public Money plannedIncome() {
        return Money.of(plannedIncome, currency);
    }

    public Money savingsTarget() {
        return savingsStrategy.create(savingsValue).savingsFor(plannedIncome());
    }

    public Money spendable() {
        return plannedIncome().minus(savingsTarget());
    }

    public Money allocatedTotal() {
        return currentAllocatedTotal();
    }

    public YearMonth period() {
        return YearMonth.of(year, month);
    }

    public boolean isOwnedBy(long userId) {
        return user.getId() != null && user.getId() == userId;
    }

    public Long getId() {
        return id;
    }

    public String getCurrency() {
        return currency;
    }

    public SavingsStrategyType getSavingsStrategy() {
        return savingsStrategy;
    }

    public BigDecimal getSavingsValue() {
        return savingsValue.stripTrailingZeros().scale() < 0
                ? savingsValue.setScale(0)
                : savingsValue.stripTrailingZeros();
    }

    public List<BudgetAllocation> getAllocations() {
        return Collections.unmodifiableList(allocations);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MonthlyBudget that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(MonthlyBudget.class);
    }

    @Override
    public String toString() {
        return "MonthlyBudget{id=%d, period=%d-%02d, currency=%s}".formatted(id, year, month, currency);
    }
}
