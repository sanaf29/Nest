package com.nest.service.insight;

import java.util.List;

/**
 * Supplies recent amount changes of recurring expenses to the insight summary. Kept as a small
 * interface so the insight feature does not depend on how recurring transactions are stored.
 */
public interface RecurringChangeSource {

    List<FinancialSummary.RecurringChange> recentChanges(long userId, String currency);
}
