package com.nest.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** Expenses for a month and currency, by category, largest first. */
public record CategorySpendingResponse(int year, int month, String currency, BigDecimal total, List<Slice> categories) {

    public record Slice(Long categoryId, String categoryName, String color, BigDecimal amount, BigDecimal percentOfTotal) {
    }
}
