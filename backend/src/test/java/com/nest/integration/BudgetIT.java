package com.nest.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BudgetIT extends AbstractIntegrationTest {

    private String token;
    private long chequing;
    private long groceries;
    private long dining;
    private long salary;

    @BeforeEach
    void setUp() throws Exception {
        token = registerAndLogin();
        chequing = openAccount(token, "Chequing", "CHECKING", "0", null);
        groceries = seededCategoryId("Groceries");
        dining = seededCategoryId("Dining Out");
        salary = seededCategoryId("Salary");
    }

    private static Map<String, Object> budget(int year, int month, Object income, String strategy, Object value,
                                              List<Map<String, Object>> allocations) {
        Map<String, Object> body = new HashMap<>();
        body.put("year", year);
        body.put("month", month);
        body.put("plannedIncome", income);
        body.put("savingsStrategy", strategy);
        body.put("savingsValue", value);
        body.put("allocations", allocations);
        return body;
    }

    private static Map<String, Object> limit(long categoryId, Object amount) {
        return Map.of("categoryId", categoryId, "limit", amount);
    }

    private ResultActions postBudget(String userToken, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post("/api/budgets").header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }

    private JsonNode createBudget(Map<String, Object> body) throws Exception {
        return body(postBudget(token, body).andExpect(status().isCreated()).andReturn());
    }

    private JsonNode summary(long budgetId) throws Exception {
        return body(mockMvc.perform(get("/api/budgets/{id}/summary", budgetId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk()).andReturn());
    }

    @Test
    void createDerivesSavingsAndSpendable() throws Exception {
        JsonNode created = createBudget(budget(2026, 10, "3000.00", "PERCENTAGE", 20,
                List.of(limit(groceries, "500"), limit(dining, "200"))));

        assertThat(created.get("currency").asText()).isEqualTo("CAD");
        assertThat(created.get("savingsTarget").decimalValue()).isEqualByComparingTo("600.00");
        assertThat(created.get("spendableAmount").decimalValue()).isEqualByComparingTo("2400.00");
        assertThat(created.get("allocatedAmount").decimalValue()).isEqualByComparingTo("700.00");
        assertThat(created.get("unallocatedAmount").decimalValue()).isEqualByComparingTo("1700.00");
        assertThat(created.get("allocations")).hasSize(2);
    }

    @Test
    void summaryCountsOnlyThisMonthsIncomeAndExpensesInTheBudgetCurrency() throws Exception {
        long savings = openAccount(token, "Savings", "SAVINGS", "0", null);
        long usd = openAccount(token, "US", "CHECKING", "0", "USD");
        long id = createBudget(budget(2026, 10, "3000", "PERCENTAGE", 20,
                List.of(limit(groceries, "500"), limit(dining, "200")))).get("id").asLong();

        record(token, chequing, salary, "INCOME", "3000.00", "2026-10-01");
        record(token, chequing, groceries, "EXPENSE", "410.50", "2026-10-31");  // last day: counted
        record(token, chequing, dining, "EXPENSE", "250.00", "2026-10-15");
        record(token, chequing, groceries, "EXPENSE", "99.00", "2026-11-01");   // next month: excluded
        record(token, chequing, groceries, "EXPENSE", "50.00", "2026-09-30");   // previous month: excluded
        record(token, usd, groceries, "EXPENSE", "1000.00", "2026-10-10");      // other currency: excluded
        transfer(token, chequing, savings, "500.00", "2026-10-20");             // transfer: excluded

        JsonNode s = summary(id);

        assertThat(s.get("actualIncome").decimalValue()).isEqualByComparingTo("3000.00");
        assertThat(s.get("actualExpenses").decimalValue()).isEqualByComparingTo("660.50");
        assertThat(s.get("netCashFlow").decimalValue()).isEqualByComparingTo("2339.50");
        assertThat(s.get("remaining").decimalValue()).isEqualByComparingTo("1739.50");
        assertThat(s.get("savingsProgressPercent").decimalValue()).isEqualByComparingTo("389.92");
        assertThat(s.get("unbudgetedSpending").decimalValue()).isEqualByComparingTo("0.00");
        JsonNode lines = s.get("categories");
        assertThat(lines.get(0).get("categoryName").asText()).isEqualTo("Groceries");
        assertThat(lines.get(0).get("percentUsed").decimalValue()).isEqualByComparingTo("82.10");
        assertThat(lines.get(0).get("status").asText()).isEqualTo("NEAR_LIMIT");
        assertThat(lines.get(1).get("status").asText()).isEqualTo("EXCEEDED");
        assertThat(lines.get(1).get("remaining").decimalValue()).isEqualByComparingTo("-50.00");
    }

    @Test
    void leapDayBelongsToFebruary() throws Exception {
        long id = createBudget(budget(2028, 2, "1000", "FIXED_AMOUNT", "100", List.of(limit(groceries, "300")))).get("id").asLong();
        record(token, chequing, groceries, "EXPENSE", "29.00", "2028-02-29");
        record(token, chequing, groceries, "EXPENSE", "1.00", "2028-03-01");

        JsonNode s = summary(id);

        assertThat(s.get("actualExpenses").decimalValue()).isEqualByComparingTo("29.00");
        assertThat(s.get("savingsTarget").decimalValue()).isEqualByComparingTo("100.00");
    }

    @Test
    void oneBudgetPerMonthAndCurrency() throws Exception {
        createBudget(budget(2026, 10, "3000", "PERCENTAGE", 10, List.of()));

        postBudget(token, budget(2026, 10, "1000", "PERCENTAGE", 10, List.of())).andExpect(status().isConflict());

        Map<String, Object> usdBudget = budget(2026, 10, "1000", "PERCENTAGE", 10, List.of());
        usdBudget.put("currency", "USD");
        postBudget(token, usdBudget).andExpect(status().isCreated());

        mockMvc.perform(get("/api/budgets/2026/10").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.currency").value("CAD"));
        mockMvc.perform(get("/api/budgets/2026/10").param("currency", "USD").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.plannedIncome").value(1000.00));
        mockMvc.perform(get("/api/budgets/2026/11").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/budgets").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void invalidPlansAreRejected() throws Exception {
        postBudget(token, budget(2026, 13, "1000", "PERCENTAGE", 10, List.of())).andExpect(status().isBadRequest());
        postBudget(token, budget(2026, 10, "-1", "PERCENTAGE", 10, List.of())).andExpect(status().isBadRequest());
        postBudget(token, budget(2026, 10, "1000", "PERCENTAGE", 120, List.of())).andExpect(status().isUnprocessableEntity());
        postBudget(token, budget(2026, 10, "1000", "FIXED_AMOUNT", 1500, List.of())).andExpect(status().isUnprocessableEntity());
        postBudget(token, budget(2026, 10, "1000", "PERCENTAGE", 20, List.of(limit(groceries, "800.01"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Category limits total 800.01 CAD, but only 800.00 CAD is available after savings."));
        postBudget(token, budget(2026, 10, "1000", "PERCENTAGE", 20, List.of(limit(salary, "100"))))
                .andExpect(status().isUnprocessableEntity());
        postBudget(token, budget(2026, 10, "1000", "PERCENTAGE", 20, List.of(limit(groceries, "100"), limit(groceries, "50"))))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/api/budgets/2026/13").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateReplacesPlanAndAllocationsKeepingRowsForKeptCategories() throws Exception {
        JsonNode created = createBudget(budget(2026, 10, "3000", "PERCENTAGE", 20,
                List.of(limit(groceries, "2000"), limit(dining, "300"))));
        long id = created.get("id").asLong();
        long groceriesRow = created.get("allocations").get(0).get("id").asLong();
        long transport = seededCategoryId("Transportation");

        // Lower income: valid only because the new limits are checked against the new plan.
        Map<String, Object> update = budget(0, 0, "2000", "PERCENTAGE", 20, List.of(limit(groceries, "1200"), limit(transport, "100")));
        JsonNode updated = body(mockMvc.perform(put("/api/budgets/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json(update)))
                .andExpect(status().isOk()).andReturn());

        assertThat(updated.get("plannedIncome").decimalValue()).isEqualByComparingTo("2000");
        assertThat(updated.get("allocations")).hasSize(2);
        assertThat(updated.get("allocations").get(0).get("id").asLong()).isEqualTo(groceriesRow);
        assertThat(updated.get("allocations").get(0).get("limit").decimalValue()).isEqualByComparingTo("1200");
        assertThat(updated.get("allocations").get(1).get("categoryName").asText()).isEqualTo("Transportation");
    }

    @Test
    void deleteRemovesThePlanOnly() throws Exception {
        long id = createBudget(budget(2026, 10, "3000", "PERCENTAGE", 20, List.of(limit(groceries, "500")))).get("id").asLong();
        long tx = record(token, chequing, groceries, "EXPENSE", "10", "2026-10-05");

        mockMvc.perform(delete("/api/budgets/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/budgets/2026/10").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/transactions/{id}", tx).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void anotherUsersBudgetAndCategoriesAreOffLimits() throws Exception {
        long id = createBudget(budget(2026, 10, "3000", "PERCENTAGE", 20, List.of())).get("id").asLong();
        String other = registerAndLogin();
        long theirCategory = body(mockMvc.perform(post("/api/categories").header(HttpHeaders.AUTHORIZATION, bearer(other))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("name", "Secret", "categoryType", "EXPENSE")))).andReturn()).get("id").asLong();

        mockMvc.perform(get("/api/budgets/{id}/summary", id).header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/budgets/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(budget(0, 0, "1", "PERCENTAGE", 0, List.of()))))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/budgets/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/budgets").header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(jsonPath("$.length()").value(0));
        postBudget(token, budget(2026, 11, "1000", "PERCENTAGE", 0, List.of(limit(theirCategory, "10"))))
                .andExpect(status().isNotFound());
    }
}
