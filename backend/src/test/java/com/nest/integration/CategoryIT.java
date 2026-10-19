package com.nest.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryIT extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static Map<String, Object> category(String name, String type, String color) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("categoryType", type);
        body.put("color", color);
        return body;
    }

    private long createCategory(String token, Map<String, Object> body) throws Exception {
        return body(mockMvc.perform(post("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andReturn()).get("id").asLong();
    }

    private long defaultCategoryId(String name) {
        return jdbcTemplate.queryForObject("SELECT id FROM categories WHERE user_id IS NULL AND name = ?", Long.class, name);
    }

    @Test
    void migrationSeedsTheDefaultCategories() {
        Integer income = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM categories WHERE user_id IS NULL AND category_type = 'INCOME'", Integer.class);
        Integer expense = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM categories WHERE user_id IS NULL AND category_type = 'EXPENSE'", Integer.class);

        assertThat(income).isEqualTo(6);
        assertThat(expense).isEqualTo(15);
    }

    @Test
    void newUserSeesDefaultsAndCanFilterByType() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/api/categories").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(21))
                .andExpect(jsonPath("$[*].isDefault", everyItem(is(true))))
                .andExpect(jsonPath("$[*].name", hasItems("Salary", "Groceries", "Housing")));

        mockMvc.perform(get("/api/categories").param("type", "INCOME").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[*].categoryType", everyItem(is("INCOME"))));
    }

    @Test
    void userCanCreateUpdateAndArchiveOwnCategory() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(post("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(category("Pets", "EXPENSE", "#f97316"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.color").value("#F97316"))
                .andExpect(jsonPath("$.isDefault").value(false))
                .andExpect(jsonPath("$.archived").value(false));
        long id = createCategory(token, category("Tutoring", "INCOME", null));

        mockMvc.perform(put("/api/categories/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Private Tutoring", "color", "#123ABC"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Private Tutoring"))
                .andExpect(jsonPath("$.categoryType").value("INCOME"));

        mockMvc.perform(delete("/api/categories/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/categories/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/categories").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$[*].name", not(hasItem("Private Tutoring"))));
        mockMvc.perform(get("/api/categories").param("includeArchived", "true").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$[?(@.name == 'Private Tutoring')].archived", hasItem(true)));
        mockMvc.perform(put("/api/categories/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Revived"))))
                .andExpect(status().isUnprocessableEntity());

        // The archived name is free again.
        createCategory(token, category("Private Tutoring", "INCOME", null));
    }

    @Test
    void namesAreUniquePerTypeIncludingDefaultsIgnoringCase() throws Exception {
        String token = registerAndLogin();
        createCategory(token, category("Pets", "EXPENSE", null));

        for (Map<String, Object> duplicate : new Map[]{
                category("PETS", "EXPENSE", null),
                category("groceries", "EXPENSE", null)}) {
            mockMvc.perform(post("/api/categories")
                            .header(HttpHeaders.AUTHORIZATION, bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(duplicate)))
                    .andExpect(status().isConflict());
        }

        // Same name, other type: allowed. Same name, other user: allowed.
        createCategory(token, category("Pets", "INCOME", null));
        createCategory(registerAndLogin(), category("Pets", "EXPENSE", null));
    }

    @Test
    void defaultCategoriesAreReadOnly() throws Exception {
        String token = registerAndLogin();
        long groceries = defaultCategoryId("Groceries");

        mockMvc.perform(get("/api/categories/{id}", groceries).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDefault").value(true));
        mockMvc.perform(put("/api/categories/{id}", groceries)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Food"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Default category 'Groceries' cannot be edited."));
        mockMvc.perform(delete("/api/categories/{id}", groceries).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());

        String name = jdbcTemplate.queryForObject("SELECT name FROM categories WHERE id = ?", String.class, groceries);
        assertThat(name).isEqualTo("Groceries");
    }

    @Test
    void anotherUsersCategoryIsInvisibleAndUntouchable() throws Exception {
        String alice = registerAndLogin();
        String bob = registerAndLogin();
        long aliceCategory = createCategory(alice, category("Alice Hobby", "EXPENSE", null));

        mockMvc.perform(get("/api/categories").header(HttpHeaders.AUTHORIZATION, bearer(bob)))
                .andExpect(jsonPath("$[*].id", not(hasItem((int) aliceCategory))));
        mockMvc.perform(get("/api/categories/{id}", aliceCategory).header(HttpHeaders.AUTHORIZATION, bearer(bob)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/categories/{id}", aliceCategory)
                        .header(HttpHeaders.AUTHORIZATION, bearer(bob))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Bob's now"))))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/categories/{id}", aliceCategory).header(HttpHeaders.AUTHORIZATION, bearer(bob)))
                .andExpect(status().isNotFound());

        JsonNode unchanged = body(mockMvc.perform(get("/api/categories/{id}", aliceCategory)
                .header(HttpHeaders.AUTHORIZATION, bearer(alice))).andReturn());
        assertThat(unchanged.get("name").asText()).isEqualTo("Alice Hobby");
        assertThat(unchanged.get("archived").asBoolean()).isFalse();
    }

    @Test
    void invalidInputIsBadRequest() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(post("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(category("", "SAVINGS", "red"))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(category("", "EXPENSE", "red"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItems("name", "color")));
        mockMvc.perform(get("/api/categories").param("type", "SAVINGS").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/categories")).andExpect(status().isUnauthorized());
    }
}
