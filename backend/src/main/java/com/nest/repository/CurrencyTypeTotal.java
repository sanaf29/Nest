package com.nest.repository;

import com.nest.model.TransactionType;

import java.math.BigDecimal;

/** Sum of one transaction type in one currency. */
public record CurrencyTypeTotal(String currency, TransactionType type, BigDecimal total) {
}
