package com.nest.model;

/**
 * How much of a month's income is set aside as savings (Strategy pattern).
 *
 * <p>Implementations are pure and stateless with respect to the database, so they can be unit
 * tested in isolation and new strategies can be added without changing {@link MonthlyBudget}
 * (open/closed).
 */
public interface SavingsAllocationStrategy {

    /**
     * @param income planned income for the month
     * @return the savings target, in the income's currency and rounded to its minor units
     */
    Money savingsFor(Money income);
}
