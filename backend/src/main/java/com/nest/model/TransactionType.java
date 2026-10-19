package com.nest.model;

import java.util.Optional;

/**
 * Kinds of ledger entry. Each constant knows its effect on an account balance, so balance and
 * report logic never needs an if/switch over types. Amounts are always stored positive; the
 * direction comes from the type.
 *
 * <p>A transfer is recorded as one {@link #TRANSFER_OUT} entry on the source account and one
 * {@link #TRANSFER_IN} entry on the destination account. Neither one is income or spending.
 */
public enum TransactionType {
    INCOME(1, false, CategoryType.INCOME),
    EXPENSE(-1, false, CategoryType.EXPENSE),
    TRANSFER_IN(1, true, null),
    TRANSFER_OUT(-1, true, null);

    private final int balanceSign;
    private final boolean transfer;
    private final CategoryType requiredCategoryType;

    TransactionType(int balanceSign, boolean transfer, CategoryType requiredCategoryType) {
        this.balanceSign = balanceSign;
        this.transfer = transfer;
        this.requiredCategoryType = requiredCategoryType;
    }

    /** +1 if this entry increases the account balance, -1 if it decreases it. */
    public int balanceSign() {
        return balanceSign;
    }

    public boolean isTransfer() {
        return transfer;
    }

    /**
     * The category type this entry must be classified with. Empty for transfer legs, which have
     * no category because moving money between your own accounts is neither income nor spending.
     */
    public Optional<CategoryType> requiredCategoryType() {
        return Optional.ofNullable(requiredCategoryType);
    }
}
