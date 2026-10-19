package com.nest.model;

/**
 * Kinds of ledger entry. Each constant knows its effect on an account balance, so balance and
 * report logic never needs an if/switch over types. Amounts are always stored positive; the
 * direction comes from the type.
 *
 * <p>A transfer is recorded as one {@link #TRANSFER_OUT} entry on the source account and one
 * {@link #TRANSFER_IN} entry on the destination account. Neither one is income or spending.
 */
public enum TransactionType {
    INCOME(1, false),
    EXPENSE(-1, false),
    TRANSFER_IN(1, true),
    TRANSFER_OUT(-1, true);

    private final int balanceSign;
    private final boolean transfer;

    TransactionType(int balanceSign, boolean transfer) {
        this.balanceSign = balanceSign;
        this.transfer = transfer;
    }

    /** +1 if this entry increases the account balance, -1 if it decreases it. */
    public int balanceSign() {
        return balanceSign;
    }

    public boolean isTransfer() {
        return transfer;
    }
}
