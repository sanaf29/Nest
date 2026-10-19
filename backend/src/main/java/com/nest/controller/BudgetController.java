package com.nest.controller;

import com.nest.dto.request.CreateBudgetRequest;
import com.nest.dto.request.UpdateBudgetRequest;
import com.nest.dto.response.BudgetResponse;
import com.nest.dto.response.BudgetSummaryResponse;
import com.nest.security.CurrentUserProvider;
import com.nest.service.BudgetService;
import com.nest.validation.ValidCurrency;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@Tag(name = "Budgets")
public class BudgetController {

    private final BudgetService budgetService;
    private final CurrentUserProvider currentUser;

    public BudgetController(BudgetService budgetService, CurrentUserProvider currentUser) {
        this.budgetService = budgetService;
        this.currentUser = currentUser;
    }

    @PostMapping
    @Operation(summary = "Create a monthly budget",
            description = "201 created; 400 invalid input; 404 unknown category; 409 a budget for this month and "
                    + "currency exists; 422 savings exceed income, limits exceed spendable, or a non-expense/archived category.")
    public ResponseEntity<BudgetResponse> create(@Valid @RequestBody CreateBudgetRequest request) {
        BudgetResponse created = budgetService.create(currentUser.currentUserId(), request);
        return ResponseEntity.created(URI.create("/api/budgets/%d/%d?currency=%s"
                .formatted(created.year(), created.month(), created.currency()))).body(created);
    }

    @GetMapping
    @Operation(summary = "List my budgets, newest month first")
    public List<BudgetResponse> list() {
        return budgetService.list(currentUser.currentUserId());
    }

    @GetMapping("/{year}/{month}")
    @Operation(summary = "Get my budget for a month", description = "currency defaults to my preferred currency. 404 if none.")
    public BudgetResponse getForMonth(@PathVariable @Min(2000) @Max(2100) int year,
                                      @PathVariable @Min(1) @Max(12) int month,
                                      @RequestParam(required = false) @ValidCurrency String currency) {
        return budgetService.getForMonth(currentUser.currentUserId(), year, month, currency);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a budget's plan and allocations", description = "Period and currency are fixed.")
    public BudgetResponse update(@PathVariable long id, @Valid @RequestBody UpdateBudgetRequest request) {
        return budgetService.update(currentUser.currentUserId(), id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a budget", description = "Removes the plan only; transactions are unaffected.")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        budgetService.delete(currentUser.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/summary")
    @Operation(summary = "Plan versus actual for the budget's month",
            description = "Income, expenses, net cash flow, remaining, savings progress, and per-category status. "
                    + "Transfers are excluded; only accounts in the budget's currency count.")
    public BudgetSummaryResponse summary(@PathVariable long id) {
        return budgetService.summary(currentUser.currentUserId(), id);
    }
}
