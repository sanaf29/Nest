package com.nest.repository;

import com.nest.model.TransactionType;

import java.math.BigDecimal;

/** One row of the balance aggregation: the sum of one transaction type on one account. */
public record AccountTypeTotal(Long accountId, TransactionType type, BigDecimal total) {
}
