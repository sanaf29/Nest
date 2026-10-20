package com.nest.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InsightIT extends AbstractIntegrationTest {

    private JsonNode insights(String token, String query) throws Exception {
        return body(mockMvc.perform(get("/api/insights?" + query).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk()).andReturn());
    }

    private static List<String> codes(JsonNode response) {
        List<String> codes = new ArrayList<>();
        response.get("insights").forEach(i -> codes.add(i.get("code").asText()));
        return codes;
    }

    @Test
    void pastMonthInsightsComeFromRealDataMostUrgentFirst() throws Exception {
        String token = registerAndLogin();
        long chequing = openAccount(token, "Chequing", "CHECKING", "0", null);
        long savings = openAccount(token, "Savings", "SAVINGS", "0", null);
        long salary = seededCategoryId("Salary");
        long dining = seededCategoryId("Dining Out");
        long groceries = seededCategoryId("Groceries");

        // February 2025: 100 spent. March 2025: 1,250 spent against 1,000 income, dining over its limit.
        record(token, chequing, groceries, "EXPENSE", "100.00", "2025-02-10");
        record(token, chequing, salary, "INCOME", "1000.00", "2025-03-01");
        record(token, chequing, dining, "EXPENSE", "250.00", "2025-03-05");
        record(token, chequing, groceries, "EXPENSE", "1000.00", "2025-03-20");
        transfer(token, chequing, savings, "999.00", "2025-03-21"); // not spending
        mockMvc.perform(post("/api/budgets").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("year", 2025, "month", 3, "plannedIncome", "1000", "savingsStrategy", "PERCENTAGE",
                                "savingsValue", 0, "allocations", List.of(Map.of("categoryId", dining, "limit", "200"))))))
                .andExpect(status().isCreated());

        JsonNode result = insights(token, "year=2025&month=3");

        assertThat(result.get("asOf").asText()).isEqualTo("2025-03-31");
        assertThat(result.get("disclaimer").asText()).contains("not financial advice");
        assertThat(codes(result)).containsExactly("BUDGET_EXCEEDED", "SPENDING_EXCEEDS_INCOME", "SPENDING_TREND_UP");
        assertThat(result.get("insights").get(1).get("message").asText())
                .isEqualTo("Expenses of 1250.00 CAD are 250.00 CAD more than your income of 1000.00 CAD.");
        assertThat(result.get("insights").get(2).get("title").asText()).isEqualTo("Spending is up 1150% compared with last month");
    }

    @Test
    void aQuietMonthHasNoInsightsAndUsersSeeOnlyTheirOwnData() throws Exception {
        String token = registerAndLogin();
        long chequing = openAccount(token, "Chequing", "CHECKING", "0", null);
        record(token, chequing, seededCategoryId("Groceries"), "EXPENSE", "5000.00", "2025-03-20");

        String other = registerAndLogin();
        assertThat(insights(other, "year=2025&month=3").get("insights")).isEmpty();
        assertThat(insights(token, "year=2025&month=4").get("insights")).isEmpty();
    }

    @Test
    void invalidParametersAreRejected() throws Exception {
        String token = registerAndLogin();
        mockMvc.perform(get("/api/insights?year=2025&month=0").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/insights")).andExpect(status().isUnauthorized());
    }
}
