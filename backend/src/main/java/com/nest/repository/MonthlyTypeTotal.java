package com.nest.repository;

import com.nest.model.TransactionType;

import java.math.BigDecimal;

/** Sum of one transaction type in one calendar month. */
public record MonthlyTypeTotal(Integer year, Integer month, TransactionType type, BigDecimal total) {
}
