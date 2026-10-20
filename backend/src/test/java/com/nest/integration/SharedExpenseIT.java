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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SharedExpenseIT extends AbstractIntegrationTest {

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = registerAndLogin();
    }

    private static Map<String, Object> person(String name, boolean me, boolean paid, Object value) {
        Map<String, Object> p = new HashMap<>(Map.of("name", name, "me", me, "paid", paid));
        if (value != null) p.put("value", value);
        return p;
    }

    private ResultActions create(String userToken, String description, String total, String method, List<Map<String, Object>> people) throws Exception {
        return mockMvc.perform(post("/api/shared-expenses").header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("description", description, "totalAmount", total, "expenseDate", "2026-10-18",
                        "splitMethod", method, "participants", people))));
    }

    @Test
    void equalSplitOfTenDollarsAmongThreeSumsExactly() throws Exception {
        JsonNode e = body(create(token, "Pizza", "10.00", "EQUAL",
                List.of(person("Me", true, true, null), person("Alex", false, false, null), person("Sam", false, false, null)))
                .andExpect(status().isCreated()).andReturn());

        assertThat(e.get("participants").findValuesAsText("share")).containsExactly("3.34", "3.33", "3.33");
        assertThat(e.get("owedToYou").decimalValue()).isEqualByComparingTo("6.66");
        assertThat(e.get("currency").asText()).isEqualTo("CAD");
        Integer sumCents = jdbc.queryForObject(
                "SELECT CAST(SUM(share_amount) * 100 AS INT) FROM expense_participants WHERE shared_expense_id = ?", Integer.class,
                e.get("id").asLong());
        assertThat(sumCents).isEqualTo(1000);
    }

    @Test
    void balancesNetAcrossExpensesPerPersonAndSettlementClearsThem() throws Exception {
        JsonNode cabin = body(create(token, "Cabin", "300", "EQUAL",
                List.of(person("Me", true, true, null), person("Alex", false, false, null), person("Sam", false, false, null))).andReturn());
        create(token, "Groceries", "60", "CUSTOM",
                List.of(person("Me", true, false, "40"), person("alex", false, true, "20"))).andExpect(status().isCreated());

        // Alex owes me 100 for the cabin; I owe Alex 40 for groceries → net +60. Sam owes me 100.
        mockMvc.perform(get("/api/shared-expenses/balances").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.person =~ /alex/i)].amount").value(org.hamcrest.Matchers.contains(60.00)))
                .andExpect(jsonPath("$[?(@.person == 'Sam')].amount").value(org.hamcrest.Matchers.contains(100.00)));

        long id = cabin.get("id").asLong();
        long sam = cabin.get("participants").get(2).get("id").asLong();
        mockMvc.perform(post("/api/shared-expenses/{id}/participants/{p}/settle", id, sam).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.participants[2].settled").value(true))
                .andExpect(jsonPath("$.owedToYou").value(100.00));
        mockMvc.perform(get("/api/shared-expenses/balances").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void percentageAndValidation() throws Exception {
        create(token, "Rent", "1000", "PERCENTAGE",
                List.of(person("Me", true, true, "60"), person("Roomie", false, false, "40")))
                .andExpect(jsonPath("$.participants[1].share").value(400.00));
        create(token, "Rent", "1000", "PERCENTAGE",
                List.of(person("Me", true, true, "60"), person("Roomie", false, false, "30")))
                .andExpect(status().isUnprocessableEntity());
        create(token, "Rent", "100", "CUSTOM",
                List.of(person("Me", true, true, "60"), person("Roomie", false, false, "30")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("The amounts add up to 90.00 CAD, but the expense is 100.00 CAD."));
        create(token, "Solo", "10", "EQUAL", List.of(person("Me", true, true, null))).andExpect(status().isBadRequest());
        create(token, "Nobody paid", "10", "EQUAL", List.of(person("Me", true, false, null), person("A", false, false, null)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void sharedExpensesArePrivate() throws Exception {
        JsonNode e = body(create(token, "Trip", "50", "EQUAL", List.of(person("Me", true, true, null), person("Alex", false, false, null))).andReturn());
        long id = e.get("id").asLong();
        String other = registerAndLogin();

        mockMvc.perform(get("/api/shared-expenses/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(other))).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/shared-expenses/{id}/participants/{p}/settle", id, e.get("participants").get(1).get("id").asLong())
                .header(HttpHeaders.AUTHORIZATION, bearer(other))).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/shared-expenses/balances").header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(delete("/api/shared-expenses/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM expense_participants WHERE shared_expense_id = ?", Integer.class, id)).isZero();
    }
}
