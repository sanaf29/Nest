package com.nest.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardIT extends AbstractIntegrationTest {

    private String token;
    private long chequing;
    private long visa;
    private long usd;

    @BeforeEach
    void setUp() throws Exception {
        token = registerAndLogin();
        chequing = openAccount(token, "Chequing", "CHECKING", "1000.00", null);
        visa = openAccount(token, "Visa", "CREDIT_CARD", "0", null);
        usd = openAccount(token, "US", "CHECKING", "50.00", "USD");
        long salary = seededCategoryId("Salary");
        long groceries = seededCategoryId("Groceries");
        long dining = seededCategoryId("Dining Out");

        record(token, chequing, salary, "INCOME", "3000.00", "2026-10-01");
        record(token, chequing, groceries, "EXPENSE", "200.00", "2026-10-05");
        record(token, visa, dining, "EXPENSE", "100.00", "2026-10-06");
        record(token, usd, groceries, "EXPENSE", "20.00", "2026-10-07");
        record(token, chequing, groceries, "EXPENSE", "75.00", "2026-08-15");
        transfer(token, chequing, visa, "100.00", "2026-10-20"); // pays the card: not spending

        mockMvc.perform(post("/api/budgets").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("year", 2026, "month", 10, "plannedIncome", "3000", "savingsStrategy", "PERCENTAGE",
                                "savingsValue", 20, "allocations", List.of(Map.of("categoryId", dining, "limit", "90"))))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/savings-goals").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Trip", "targetAmount", "1000"))))
                .andExpect(status().isCreated());
    }

    private JsonNode getJson(String url) throws Exception {
        return body(mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk()).andReturn());
    }

    @Test
    void summaryReportsPerCurrencyTotalsAndExcludesTransfers() throws Exception {
        JsonNode s = getJson("/api/dashboard/summary?year=2026&month=10");

        // CAD: chequing 1000 + 3000 − 200 − 75 − 100 = 3625; visa −100 + 100 = 0 → 3625. USD: 50 − 20 = 30.
        assertThat(s.get("balances")).hasSize(2);
        assertThat(s.get("balances").get(0).get("currency").asText()).isEqualTo("CAD");
        assertThat(s.get("balances").get(0).get("total").decimalValue()).isEqualByComparingTo("3625.00");
        assertThat(s.get("balances").get(1).get("total").decimalValue()).isEqualByComparingTo("30.00");

        JsonNode cad = s.get("cashFlow").get(0);
        assertThat(cad.get("income").decimalValue()).isEqualByComparingTo("3000.00");
        assertThat(cad.get("expenses").decimalValue()).isEqualByComparingTo("300.00");
        assertThat(cad.get("net").decimalValue()).isEqualByComparingTo("2700.00");
        assertThat(s.get("cashFlow").get(1).get("expenses").decimalValue()).isEqualByComparingTo("20.00");

        JsonNode budget = s.get("budget");
        assertThat(budget.get("spent").decimalValue()).isEqualByComparingTo("300.00");
        assertThat(budget.get("remaining").decimalValue()).isEqualByComparingTo("2100.00");
        assertThat(budget.get("categoriesExceeded").asInt()).isEqualTo(1);
        assertThat(s.get("activeGoals")).hasSize(1);
        assertThat(s.get("achievedGoalCount").asLong()).isZero();
    }

    @Test
    void spendingByCategoryForAMonthAndCurrency() throws Exception {
        JsonNode cad = getJson("/api/dashboard/spending-by-category?year=2026&month=10");
        assertThat(cad.get("total").decimalValue()).isEqualByComparingTo("300.00");
        assertThat(cad.get("categories").get(0).get("categoryName").asText()).isEqualTo("Groceries");
        assertThat(cad.get("categories").get(0).get("percentOfTotal").decimalValue()).isEqualByComparingTo("66.67");

        JsonNode usdSpending = getJson("/api/dashboard/spending-by-category?year=2026&month=10&currency=USD");
        assertThat(usdSpending.get("total").decimalValue()).isEqualByComparingTo("20.00");
    }

    @Test
    void trendsCoverRequestedMonthsWithGapsAsZero() throws Exception {
        JsonNode trends = getJson("/api/dashboard/monthly-trends?months=3&year=2026&month=10");

        assertThat(trends.get("months")).hasSize(3);
        assertThat(trends.get("months").get(0).get("expenses").decimalValue()).isEqualByComparingTo("75.00");  // Aug
        assertThat(trends.get("months").get(1).get("expenses").decimalValue()).isEqualByComparingTo("0.00");   // Sep
        assertThat(trends.get("months").get(2).get("net").decimalValue()).isEqualByComparingTo("2700.00");     // Oct
    }

    @Test
    void anotherUserSeesOnlyTheirOwnEmptyDashboard() throws Exception {
        String other = registerAndLogin();

        JsonNode s = body(mockMvc.perform(get("/api/dashboard/summary?year=2026&month=10")
                .header(HttpHeaders.AUTHORIZATION, bearer(other))).andExpect(status().isOk()).andReturn());

        assertThat(s.get("balances")).isEmpty();
        assertThat(s.get("cashFlow")).isEmpty();
        assertThat(s.get("budget").isNull()).isTrue();
        assertThat(s.get("activeGoals")).isEmpty();
    }

    @Test
    void invalidParametersAreBadRequests() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary?year=2026&month=13").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/dashboard/monthly-trends?months=25").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/dashboard/spending-by-category?currency=XYZ").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/dashboard/summary")).andExpect(status().isUnauthorized());
    }
}
