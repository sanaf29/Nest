package com.nest.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountIT extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static Map<String, Object> account(String name, String type, Object openingBalance) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("accountType", type);
        body.put("openingBalance", openingBalance);
        return body;
    }

    private long createAccount(String token, Map<String, Object> body) throws Exception {
        JsonNode created = body(mockMvc.perform(post("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andReturn());
        return created.get("id").asLong();
    }

    @Test
    void createReturnsAccountWithDerivedBalanceAndDefaultCurrency() throws Exception {
        String token = registerAndLogin(); // registered with preferred currency CAD

        mockMvc.perform(post("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(account("Everyday", "CHECKING", 1500))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/accounts/\\d+")))
                .andExpect(jsonPath("$.currency").value("CAD"))
                .andExpect(jsonPath("$.openingBalance").value(1500.00))
                .andExpect(jsonPath("$.currentBalance").value(1500.00))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void storedAmountsRoundTripExactly() throws Exception {
        String token = registerAndLogin();
        long id = createAccount(token, account("Visa", "CREDIT_CARD", "-1234.56"));

        mockMvc.perform(get("/api/accounts/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openingBalance").value(-1234.56))
                .andExpect(jsonPath("$.currentBalance").value(-1234.56))
                .andExpect(jsonPath("$.accountType").value("CREDIT_CARD"));
    }

    @Test
    void domainRulesAreReportedAs422() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(post("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(account("Savings", "SAVINGS", -5))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("A savings account cannot have a negative opening balance."));

        Map<String, Object> yen = account("Yen", "CASH", "10.5");
        yen.put("currency", "JPY");
        mockMvc.perform(post("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(yen)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void malformedRequestsAreReportedAs400() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(post("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "", "accountType", "CHECKING"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("name")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("openingBalance")));

        mockMvc.perform(post("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(account("Crypto", "BITCOIN_WALLET", 0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void openAccountNamesAreUniquePerUserIgnoringCase() throws Exception {
        String token = registerAndLogin();
        createAccount(token, account("Wallet", "CASH", 0));

        mockMvc.perform(post("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(account("WALLET", "CASH", 0))))
                .andExpect(status().isConflict());

        // A different user may use the same name.
        createAccount(registerAndLogin(), account("Wallet", "CASH", 0));
    }

    @Test
    void updateChangesNameAndOpeningBalanceOnly() throws Exception {
        String token = registerAndLogin();
        long id = createAccount(token, account("Chequing", "CHECKING", 100));
        Map<String, Object> update = new HashMap<>(Map.of("name", "Main Chequing", "openingBalance", 250.75));
        update.put("currency", "USD");       // ignored: currency is immutable
        update.put("accountType", "SAVINGS"); // ignored: type is immutable

        mockMvc.perform(put("/api/accounts/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Main Chequing"))
                .andExpect(jsonPath("$.currentBalance").value(250.75))
                .andExpect(jsonPath("$.currency").value("CAD"))
                .andExpect(jsonPath("$.accountType").value("CHECKING"));
    }

    @Test
    void deleteClosesTheAccountAndKeepsIt() throws Exception {
        String token = registerAndLogin();
        long id = createAccount(token, account("Old Visa", "CREDIT_CARD", 0));

        mockMvc.perform(delete("/api/accounts/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/accounts/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent()); // idempotent

        mockMvc.perform(get("/api/accounts").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$[*].id", not(hasItem((int) id))));
        mockMvc.perform(get("/api/accounts").param("includeInactive", "true").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$[*].id", hasItem((int) id)));
        mockMvc.perform(get("/api/accounts/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // Closed accounts cannot be edited...
        mockMvc.perform(put("/api/accounts/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Reopened", "openingBalance", 0))))
                .andExpect(status().isUnprocessableEntity());
        // ...and their name is free for a new account.
        createAccount(token, account("Old Visa", "CREDIT_CARD", 0));
    }

    @Test
    void anotherUsersAccountIsInvisibleAndUntouchable() throws Exception {
        String alice = registerAndLogin();
        String bob = registerAndLogin();
        long aliceAccount = createAccount(alice, account("Alice Savings", "SAVINGS", 5000));

        mockMvc.perform(get("/api/accounts/{id}", aliceAccount).header(HttpHeaders.AUTHORIZATION, bearer(bob)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/accounts/{id}", aliceAccount)
                        .header(HttpHeaders.AUTHORIZATION, bearer(bob))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Mine now", "openingBalance", 0))))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/accounts/{id}", aliceAccount).header(HttpHeaders.AUTHORIZATION, bearer(bob)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/accounts").header(HttpHeaders.AUTHORIZATION, bearer(bob)))
                .andExpect(jsonPath("$[*].id", not(hasItem((int) aliceAccount))));

        // Alice's account is unchanged.
        mockMvc.perform(get("/api/accounts/{id}", aliceAccount).header(HttpHeaders.AUTHORIZATION, bearer(alice)))
                .andExpect(jsonPath("$.name").value("Alice Savings"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.currentBalance").value(5000.00));
    }

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/accounts")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content(json(account("X", "CASH", 0))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void databaseConstraintsBackUpTheDomainRules() throws Exception {
        String email = uniqueEmail();
        register(email);
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        String insert = "INSERT INTO financial_accounts (user_id, name, account_type, currency, opening_balance, created_at, updated_at) "
                + "VALUES (?, ?, ?, 'CAD', ?, SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET())";

        assertThatThrownBy(() -> jdbcTemplate.update(insert, userId, "Bad savings", "SAVINGS", -1))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(insert, userId, "Bad type", "GOLD", 0))
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbcTemplate.update(insert, userId, "Twin", "CASH", 0);
        assertThatThrownBy(() -> jdbcTemplate.update(insert, userId, "twin", "CASH", 0))
                .as("filtered unique index on open account names")
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
