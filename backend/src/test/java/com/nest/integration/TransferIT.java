package com.nest.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransferIT extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String token;
    private long chequing;
    private long savings;

    @BeforeEach
    void setUp() throws Exception {
        token = registerAndLogin();
        chequing = createAccount(token, "Chequing", "CHECKING", "1000.00", null);
        savings = createAccount(token, "Savings", "SAVINGS", "0", null);
    }

    private long createAccount(String userToken, String name, String type, String opening, String currency) throws Exception {
        Map<String, Object> body = new HashMap<>(Map.of("name", name, "accountType", type, "openingBalance", opening));
        if (currency != null) {
            body.put("currency", currency);
        }
        return body(mockMvc.perform(post("/api/accounts").header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
    }

    private static Map<String, Object> transfer(long from, long to, String amount) {
        Map<String, Object> body = new HashMap<>();
        body.put("sourceAccountId", from);
        body.put("destinationAccountId", to);
        body.put("amount", amount);
        body.put("transferDate", "2026-10-19");
        return body;
    }

    private ResultActions postTransfer(String userToken, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post("/api/transfers").header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }

    private JsonNode createTransfer(Map<String, Object> body) throws Exception {
        return body(postTransfer(token, body).andExpect(status().isCreated()).andReturn());
    }

    private double balance(long accountId) throws Exception {
        return body(mockMvc.perform(get("/api/accounts/{id}", accountId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andReturn()).get("currentBalance").asDouble();
    }

    private int transactionCount(long accountId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transactions WHERE account_id = ?", Integer.class, accountId);
    }

    @Test
    void transferMovesMoneyWithoutCreatingIncomeOrSpending() throws Exception {
        JsonNode created = createTransfer(transfer(chequing, savings, "250.50"));

        assertThat(created.get("sourceAccountName").asText()).isEqualTo("Chequing");
        assertThat(created.get("destinationAccountName").asText()).isEqualTo("Savings");
        assertThat(created.get("amount").decimalValue()).isEqualByComparingTo("250.50");
        assertThat(created.get("currency").asText()).isEqualTo("CAD");
        assertThat(balance(chequing)).isEqualTo(749.50);
        assertThat(balance(savings)).isEqualTo(250.50);

        long transferId = created.get("id").asLong();
        mockMvc.perform(get("/api/transactions").param("type", "TRANSFER_OUT").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].transferId").value(transferId))
                .andExpect(jsonPath("$.content[0].categoryId").doesNotExist())
                .andExpect(jsonPath("$.content[0].description").value("Transfer to Savings"));
        for (String reportType : new String[]{"INCOME", "EXPENSE"}) {
            mockMvc.perform(get("/api/transactions").param("type", reportType).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Test
    void transferCanBeReadAndDeletingItRemovesBothEntries() throws Exception {
        JsonNode created = createTransfer(transfer(chequing, savings, "100"));
        long id = created.get("id").asLong();

        mockMvc.perform(get("/api/transfers/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outgoingTransactionId").value(created.get("outgoingTransactionId").asLong()))
                .andExpect(jsonPath("$.incomingTransactionId").value(created.get("incomingTransactionId").asLong()));

        mockMvc.perform(delete("/api/transfers/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        assertThat(transactionCount(chequing)).isZero();
        assertThat(transactionCount(savings)).isZero();
        assertThat(balance(chequing)).isEqualTo(1000.00);
        mockMvc.perform(get("/api/transfers/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void transferEntriesCannotBeChangedThroughTheTransactionsApi() throws Exception {
        JsonNode created = createTransfer(transfer(chequing, savings, "100"));
        long outgoing = created.get("outgoingTransactionId").asLong();
        long groceries = jdbcTemplate.queryForObject(
                "SELECT id FROM categories WHERE user_id IS NULL AND name = 'Groceries'", Long.class);

        mockMvc.perform(put("/api/transactions/{id}", outgoing).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("accountId", chequing, "categoryId", groceries, "type", "EXPENSE",
                                "amount", "1", "transactionDate", "2026-10-19"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("/api/transfers/")));
        mockMvc.perform(delete("/api/transactions/{id}", outgoing).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict());

        assertThat(transactionCount(chequing)).isEqualTo(1);
        assertThat(balance(chequing)).isEqualTo(900.00);
    }

    @Test
    void invalidTransfersAreRejectedAndLeaveNoPartialEntries() throws Exception {
        long usd = createAccount(token, "US Account", "CHECKING", "0", "USD");
        long closed = createAccount(token, "Old", "CHECKING", "0", null);
        mockMvc.perform(delete("/api/accounts/{id}", closed).header(HttpHeaders.AUTHORIZATION, bearer(token)));

        postTransfer(token, transfer(chequing, chequing, "10")).andExpect(status().isUnprocessableEntity());
        postTransfer(token, transfer(chequing, usd, "10")).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Cannot transfer from CAD to USD: currency conversion is not supported."));
        postTransfer(token, transfer(chequing, closed, "10")).andExpect(status().isUnprocessableEntity());
        postTransfer(token, transfer(chequing, savings, "10.555")).andExpect(status().isUnprocessableEntity());
        postTransfer(token, transfer(chequing, savings, "0")).andExpect(status().isBadRequest());
        postTransfer(token, Map.of("amount", "10")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field",
                        containsInAnyOrder("sourceAccountId", "destinationAccountId", "transferDate")));

        assertThat(transactionCount(chequing)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM transactions WHERE account_id IN (?, ?, ?)",
                Integer.class, savings, usd, closed)).isZero();
    }

    @Test
    void transferOnAClosedAccountCannotBeDeleted() throws Exception {
        long id = createTransfer(transfer(chequing, savings, "10")).get("id").asLong();
        mockMvc.perform(delete("/api/accounts/{id}", savings).header(HttpHeaders.AUTHORIZATION, bearer(token)));

        mockMvc.perform(delete("/api/transfers/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableEntity());
        assertThat(transactionCount(chequing)).isEqualTo(1);
    }

    @Test
    void creditCardPaymentIsATransfer() throws Exception {
        long visa = createAccount(token, "Visa", "CREDIT_CARD", "-300.00", null);

        createTransfer(transfer(chequing, visa, "200"));

        assertThat(balance(visa)).isEqualTo(-100.00);
        assertThat(balance(chequing)).isEqualTo(800.00);
    }

    @Test
    void anotherUserCannotUseOrSeeMyAccountsOrTransfers() throws Exception {
        long id = createTransfer(transfer(chequing, savings, "10")).get("id").asLong();
        String other = registerAndLogin();
        long theirs = createAccount(other, "Theirs", "CHECKING", "0", null);

        postTransfer(other, transfer(chequing, theirs, "10")).andExpect(status().isNotFound()); // drain my account
        postTransfer(other, transfer(theirs, chequing, "10")).andExpect(status().isNotFound()); // push into my account
        mockMvc.perform(get("/api/transfers/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/transfers/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isNotFound());

        assertThat(balance(chequing)).isEqualTo(990.00);
        assertThat(transactionCount(theirs)).isZero();
    }

    @Test
    void databaseEnforcesTheShapeOfEntries() throws Exception {
        long id = createTransfer(transfer(chequing, savings, "10")).get("id").asLong();
        String insert = "INSERT INTO transactions (account_id, category_id, transfer_id, transaction_type, amount, "
                + "transaction_date, created_at, updated_at) VALUES (?, ?, ?, ?, 1, '2026-10-19', SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET())";

        assertThatThrownBy(() -> jdbcTemplate.update(insert, chequing, null, null, "EXPENSE"))
                .as("income/expense need a category").isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(insert, chequing, null, null, "TRANSFER_IN"))
                .as("transfer entries need a transfer").isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(insert, savings, null, id, "TRANSFER_IN"))
                .as("only one incoming entry per transfer").isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(post("/api/transfers").contentType(MediaType.APPLICATION_JSON)
                        .content(json(transfer(1, 2, "10"))))
                .andExpect(status().isUnauthorized());
    }
}
