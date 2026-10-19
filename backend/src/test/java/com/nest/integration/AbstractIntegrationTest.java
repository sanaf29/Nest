package com.nest.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for HTTP-level integration tests: full application context, real security filter
 * chain, real SQL Server. Each test creates its own users with unique emails, so tests do not
 * depend on each other or on execution order.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    protected static final String PASSWORD = "Str0ngPassw0rd";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbc;

    protected static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    protected String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    protected JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected void register(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "firstName", "Test",
                                "lastName", "User",
                                "email", email,
                                "password", PASSWORD,
                                "preferredCurrency", "CAD"))))
                .andExpect(status().isCreated());
    }

    /** Registers a fresh user and returns a bearer token for them. */
    protected String registerAndLogin() throws Exception {
        String email = uniqueEmail();
        register(email);
        return login(email, PASSWORD);
    }

    protected String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).get("accessToken").asText();
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    // ---- Shared fixtures (Sprint 2+) -----------------------------------------------------------

    /** Opens an account and returns its id. {@code currency} may be null for the user's preferred currency. */
    protected long openAccount(String token, String name, String type, String openingBalance, String currency) throws Exception {
        Map<String, Object> body = new HashMap<>(Map.of("name", name, "accountType", type, "openingBalance", openingBalance));
        if (currency != null) {
            body.put("currency", currency);
        }
        return body(mockMvc.perform(post("/api/accounts").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
    }

    /** Id of a seeded default category, e.g. "Groceries" or "Salary". */
    protected long seededCategoryId(String name) {
        return jdbc.queryForObject("SELECT id FROM categories WHERE user_id IS NULL AND name = ?", Long.class, name);
    }

    /** Records an INCOME or EXPENSE transaction and returns its id. */
    protected long record(String token, long accountId, long categoryId, String type, String amount, String date) throws Exception {
        Map<String, Object> body = Map.of("accountId", accountId, "categoryId", categoryId, "type", type,
                "amount", amount, "transactionDate", date);
        return body(mockMvc.perform(post("/api/transactions").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
    }

    protected void transfer(String token, long from, long to, String amount, String date) throws Exception {
        Map<String, Object> body = Map.of("sourceAccountId", from, "destinationAccountId", to, "amount", amount,
                "transferDate", date);
        mockMvc.perform(post("/api/transfers").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated());
    }
}
