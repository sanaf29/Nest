package com.nest.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** Income and expenses per month, oldest first. Months without activity are included with zeros. */
public record MonthlyTrendsResponse(String currency, List<MonthTotals> months) {

    public record MonthTotals(int year, int month, BigDecimal income, BigDecimal expenses, BigDecimal net) {
    }
}
