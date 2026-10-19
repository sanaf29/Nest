package com.nest.repository;

import com.nest.model.TransactionType;

import java.math.BigDecimal;

/** Sum of one transaction type. */
public record TypeTotal(TransactionType type, BigDecimal total) {
}
