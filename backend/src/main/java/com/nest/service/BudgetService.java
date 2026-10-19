package com.nest.service;

import com.nest.dto.request.CreateBudgetRequest;
import com.nest.dto.request.UpdateBudgetRequest;
import com.nest.dto.response.BudgetResponse;
import com.nest.dto.response.BudgetSummaryResponse;

import java.util.List;

/** Monthly budgets of the authenticated user. */
public interface BudgetService {

    BudgetResponse create(long userId, CreateBudgetRequest request);

    List<BudgetResponse> list(long userId);

    /** @param currency {@code null} for the user's preferred currency */
    BudgetResponse getForMonth(long userId, int year, int month, String currency);

    BudgetResponse update(long userId, long budgetId, UpdateBudgetRequest request);

    void delete(long userId, long budgetId);

    /** Plan versus actual spending for the budget's month and currency. */
    BudgetSummaryResponse summary(long userId, long budgetId);
}
