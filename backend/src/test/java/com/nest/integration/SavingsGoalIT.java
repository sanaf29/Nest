package com.nest.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SavingsGoalIT extends AbstractIntegrationTest {

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = registerAndLogin();
    }

    private ResultActions postGoal(String userToken, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post("/api/savings-goals").header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }

    private long createGoal(String name, String target) throws Exception {
        return body(postGoal(token, Map.of("name", name, "targetAmount", target)).andExpect(status().isCreated()).andReturn())
                .get("id").asLong();
    }

    private ResultActions contribute(long goalId, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post("/api/savings-goals/{id}/contributions", goalId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }

    @Test
    void createAndTrackProgressUntilAchieved() throws Exception {
        long id = createGoal("Emergency fund", "1000.00");

        contribute(id, Map.of("amount", "600")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.savedAmount").value(600.00))
                .andExpect(jsonPath("$.progressPercent").value(60.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        contribute(id, Map.of("amount", "450.50", "note", "Bonus")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACHIEVED"))
                .andExpect(jsonPath("$.progressPercent").value(100.00))
                .andExpect(jsonPath("$.remainingAmount").value(0.00))
                .andExpect(jsonPath("$.savedAmount").value(1050.50))
                .andExpect(jsonPath("$.achievedAt").isNotEmpty())
                .andExpect(jsonPath("$.contributions.length()").value(2));

        mockMvc.perform(get("/api/savings-goals").param("status", "ACHIEVED").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/savings-goals").param("status", "ACTIVE").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void sameIdempotencyKeyIsCountedOnce() throws Exception {
        long id = createGoal("Laptop", "2000");
        Map<String, Object> request = Map.of("amount", "100", "idempotencyKey", "3f0b6c1e-6d2a-4a8e");

        contribute(id, request).andExpect(status().isCreated()).andExpect(jsonPath("$.savedAmount").value(100.00));
        contribute(id, request).andExpect(status().isOk()).andExpect(jsonPath("$.savedAmount").value(100.00));

        Integer rows = jdbc.queryForObject("SELECT COUNT(*) FROM goal_contributions WHERE goal_id = ?", Integer.class, id);
        assertThat(rows).isEqualTo(1);
    }

    @Test
    void removingAContributionAndRaisingTheTargetReopenAGoal() throws Exception {
        long id = createGoal("Bike", "500");
        JsonNode goal = body(contribute(id, Map.of("amount", "500")).andReturn());
        long contributionId = goal.get("contributions").get(0).get("id").asLong();

        mockMvc.perform(delete("/api/savings-goals/{id}/contributions/{cid}", id, contributionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.savedAmount").value(0.00));

        contribute(id, Map.of("amount", "500")).andExpect(jsonPath("$.status").value("ACHIEVED"));
        mockMvc.perform(put("/api/savings-goals/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Better bike", "targetAmount", "800"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.progressPercent").value(62.50));
    }

    @Test
    void invalidGoalsAndContributionsAreRejected() throws Exception {
        String yesterday = LocalDate.now().minusDays(2).toString();
        String tomorrow = LocalDate.now().plusDays(2).toString();
        postGoal(token, Map.of("name", "", "targetAmount", "0")).andExpect(status().isBadRequest());
        postGoal(token, Map.of("name", "Late", "targetAmount", "10", "targetDate", yesterday))
                .andExpect(status().isUnprocessableEntity());
        long id = createGoal("Car", "10000");
        postGoal(token, Map.of("name", "car", "targetAmount", "5")).andExpect(status().isConflict());

        contribute(id, Map.of("amount", "-5")).andExpect(status().isBadRequest());
        contribute(id, Map.of("amount", "5", "contributionDate", tomorrow)).andExpect(status().isUnprocessableEntity());
        contribute(id, Map.of("amount", "5.001")).andExpect(status().isUnprocessableEntity());
        Map<String, Object> badKey = new HashMap<>(Map.of("amount", "5", "idempotencyKey", "no spaces allowed"));
        contribute(id, badKey).andExpect(status().isBadRequest());
    }

    @Test
    void anotherUsersGoalIsOffLimits() throws Exception {
        long id = createGoal("Private", "100");
        String other = registerAndLogin();

        mockMvc.perform(get("/api/savings-goals/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/savings-goals/{id}/contributions", id).header(HttpHeaders.AUTHORIZATION, bearer(other))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("amount", "1"))))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/savings-goals/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/savings-goals").header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deleteRemovesGoalAndContributions() throws Exception {
        long id = createGoal("Temp", "100");
        contribute(id, Map.of("amount", "10"));

        mockMvc.perform(delete("/api/savings-goals/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM goal_contributions WHERE goal_id = ?", Integer.class, id)).isZero();
    }
}
