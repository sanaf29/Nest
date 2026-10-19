package com.nest.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionTypeTest {

    @Test
    void inflowsIncreaseAndOutflowsDecreaseTheBalance() {
        assertThat(TransactionType.INCOME.balanceSign()).isPositive();
        assertThat(TransactionType.TRANSFER_IN.balanceSign()).isPositive();
        assertThat(TransactionType.EXPENSE.balanceSign()).isNegative();
        assertThat(TransactionType.TRANSFER_OUT.balanceSign()).isNegative();
    }

    @Test
    void onlyTransferLegsAreTransfers() {
        assertThat(TransactionType.TRANSFER_IN.isTransfer()).isTrue();
        assertThat(TransactionType.TRANSFER_OUT.isTransfer()).isTrue();
        assertThat(TransactionType.INCOME.isTransfer()).isFalse();
        assertThat(TransactionType.EXPENSE.isTransfer()).isFalse();
    }

    @Test
    void incomeAndExpenseRequireMatchingCategoryTypeWhileTransfersHaveNone() {
        assertThat(TransactionType.INCOME.requiredCategoryType()).contains(CategoryType.INCOME);
        assertThat(TransactionType.EXPENSE.requiredCategoryType()).contains(CategoryType.EXPENSE);
        assertThat(TransactionType.TRANSFER_IN.requiredCategoryType()).isEmpty();
        assertThat(TransactionType.TRANSFER_OUT.requiredCategoryType()).isEmpty();
    }
}
