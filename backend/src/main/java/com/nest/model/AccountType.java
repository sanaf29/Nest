package com.nest.model;

/**
 * Kinds of financial account. All types share one sign convention:
 * positive = money you have, negative = money you owe.
 *
 * <p>The types differ only in this rule, not in state or behaviour, so an enum with behaviour
 * is used instead of a JPA inheritance hierarchy.
 */
public enum AccountType {
    CHECKING(true),     // may start overdrawn
    SAVINGS(false),
    CASH(false),        // physical cash cannot be negative
    CREDIT_CARD(true);  // a negative balance is the amount owed

    private final boolean negativeOpeningBalanceAllowed;

    AccountType(boolean negativeOpeningBalanceAllowed) {
        this.negativeOpeningBalanceAllowed = negativeOpeningBalanceAllowed;
    }

    public boolean allowsNegativeOpeningBalance() {
        return negativeOpeningBalanceAllowed;
    }
}
