package com.nest.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportIT extends AbstractIntegrationTest {

    private String token;
    private long chequing;

    @BeforeEach
    void setUp() throws Exception {
        token = registerAndLogin();
        chequing = openAccount(token, "Chequing", "CHECKING", "0", null);
        long savings = openAccount(token, "Savings", "SAVINGS", "0", null);
        record(token, chequing, seededCategoryId("Salary"), "INCOME", "3000.00", "2026-10-01");
        record(token, chequing, seededCategoryId("Groceries"), "EXPENSE", "300.00", "2026-10-05");
        record(token, chequing, seededCategoryId("Dining Out"), "EXPENSE", "100.00", "2026-10-06");
        record(token, chequing, seededCategoryId("Groceries"), "EXPENSE", "50.00", "2026-09-10");
        transfer(token, chequing, savings, "500.00", "2026-10-07");
        mockMvc.perform(post("/api/transactions").header(HttpHeaders.AUTHORIZATION, bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("accountId", chequing, "categoryId", seededCategoryId("Shopping"), "type", "EXPENSE",
                        "amount", "20.00", "transactionDate", "2026-10-08", "description", "=HYPERLINK(\"http://evil.example\",\"click\")"))));
    }

    @Test
    void monthlyReportSummarisesTheMonthAndComparesWithTheLast() throws Exception {
        JsonNode r = body(mockMvc.perform(get("/api/reports/monthly?year=2026&month=10").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk()).andReturn());

        assertThat(r.get("totals").get("income").decimalValue()).isEqualByComparingTo("3000");
        assertThat(r.get("totals").get("expenses").decimalValue()).isEqualByComparingTo("420"); // transfer excluded
        assertThat(r.get("previousMonth").get("expenses").decimalValue()).isEqualByComparingTo("50");
        assertThat(r.get("expensesByCategory").get(0).get("category").asText()).isEqualTo("Groceries");
        assertThat(r.get("expensesByCategory").get(0).get("percentOfTotal").decimalValue()).isEqualByComparingTo("71.43");
        assertThat(r.get("incomeByCategory").get(0).get("category").asText()).isEqualTo("Salary");
        assertThat(r.get("topExpenses")).hasSize(3);
        assertThat(r.get("topExpenses").get(0).get("amount").decimalValue()).isEqualByComparingTo("300");
        assertThat(r.get("budget").isNull()).isTrue();
    }

    @Test
    void transactionsCsvIsAnAttachmentWithSafeCellsAndOnlyMyData() throws Exception {
        String other = registerAndLogin();
        long theirs = openAccount(other, "Secret account", "CHECKING", "0", null);
        record(other, theirs, seededCategoryId("Groceries"), "EXPENSE", "999.00", "2026-10-05");

        MvcResult result = mockMvc.perform(get("/api/reports/transactions.csv?from=2026-10-01&to=2026-10-31")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"nest-transactions-2026-10-01_2026-10-31.csv\""))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8"))
                .andReturn();
        String csv = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        String[] lines = csv.split("\r\n");

        assertThat(lines[0]).isEqualTo("﻿\"Date\",\"Type\",\"Account\",\"Category\",\"Description\",\"Amount\",\"Currency\",\"Transfer ID\"");
        assertThat(lines).hasSize(7); // header + income + 2 expenses + 2 transfer legs + shopping
        assertThat(lines[1]).startsWith("2026-10-01,\"INCOME\",\"Chequing\",\"Salary\",,3000.00,\"CAD\",");
        assertThat(csv).contains(",-300.00,\"CAD\"");
        assertThat(csv).contains("\"'=HYPERLINK(\"\"http://evil.example\"\",\"\"click\"\")\"");
        assertThat(csv).doesNotContain("Secret account").doesNotContain("999.00");
    }

    @Test
    void monthlyCsvAndValidation() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/reports/monthly.csv?year=2026&month=10").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk()).andReturn();
        String csv = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(csv).contains("\"Summary\",\"Net cash flow\",2580.00,\"CAD\",");
        assertThat(csv).contains("\"Expenses by category\",\"Groceries\",300.00,\"CAD\",71.43");

        mockMvc.perform(get("/api/reports/monthly.csv?year=2026&month=13").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/reports/transactions.csv?from=2026-10-31&to=2026-10-01").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/reports/transactions.csv")).andExpect(status().isUnauthorized());
    }
}
