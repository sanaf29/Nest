package com.nest.service.insight.rules;

import com.nest.service.insight.FinancialInsight;
import com.nest.service.insight.FinancialInsightRule;
import com.nest.service.insight.FinancialSummary;
import com.nest.service.insight.InsightSeverity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Month-to-date spending at least 20 % above the same days of last month. Comparing like-for-like
 * periods avoids a false "spending dropped" early in the month and a false rise late in it.
 */
@Component
public class SpendingTrendRule implements FinancialInsightRule {

    static final BigDecimal THRESHOLD_PERCENT = BigDecimal.valueOf(20);

    @Override
    public Optional<FinancialInsight> evaluate(FinancialSummary summary) {
        if (!summary.expensesSamePeriodLastMonth().isPositive()) {
            return Optional.empty();
        }
        // Compare exactly (current × 100 ≥ previous × 120); rounding is only for the message,
        // so 19.8 % is never reported as crossing a 20 % threshold.
        BigDecimal current = summary.expensesToDate().amount().multiply(BigDecimal.valueOf(100));
        BigDecimal threshold = summary.expensesSamePeriodLastMonth().amount().multiply(BigDecimal.valueOf(100).add(THRESHOLD_PERCENT));
        if (current.compareTo(threshold) < 0) {
            return Optional.empty();
        }
        BigDecimal increase = Percentages.increase(summary.expensesToDate(), summary.expensesSamePeriodLastMonth());
        int days = summary.asOf().getDayOfMonth();
        return Optional.of(new FinancialInsight("SPENDING_TREND_UP", InsightSeverity.INFO,
                "Spending is up %s compared with last month".formatted(Percentages.whole(increase)),
                "In the first %d day%s of this month you spent %s, compared with %s over the same days last month."
                        .formatted(days, days == 1 ? "" : "s", summary.expensesToDate(), summary.expensesSamePeriodLastMonth())));
    }
}
