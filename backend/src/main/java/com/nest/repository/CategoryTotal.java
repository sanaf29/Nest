package com.nest.repository;

import java.math.BigDecimal;

/** Sum of expenses in one category. */
public record CategoryTotal(Long categoryId, String categoryName, String color, BigDecimal total) {
}
