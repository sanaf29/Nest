package com.nest.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.nest.model.User;
import com.nest.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIT extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    private Map<String, Object> registration(String email) {
        Map<String, Object> body = new HashMap<>();
        body.put("firstName", "Sanae");
        body.put("lastName", "Faghfouri");
        body.put("email", email);
        body.put("password", PASSWORD);
        body.put("preferredCurrency", "CAD");
        return body;
    }

    @Test
    void registerCreatesUserWithHashedPasswordAndNeverReturnsIt() throws Exception {
        String email = uniqueEmail();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(registration(email.toUpperCase()))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/users/me"))
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.preferredCurrency").value("CAD"))
                .andExpect(jsonPath("$.createdAt").value(notNullValue()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain(PASSWORD);
        User stored = userRepository.findByEmail(email).orElseThrow();
        assertThat(stored.passwordMatches(PASSWORD, new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()))
                .as("stored value is a BCrypt hash of the password")
                .isTrue();
    }

    @Test
    void duplicateEmailInAnyCaseIsRejectedWithConflict() throws Exception {
        String email = uniqueEmail();
        register(email);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(registration(email.toUpperCase()))))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("An account with this email already exists."));
    }

    @Test
    void invalidRegistrationReportsEveryInvalidField() throws Exception {
        Map<String, Object> body = registration("not-an-email");
        body.put("password", "short");
        body.put("preferredCurrency", "XYZ");
        body.put("firstName", "");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()))
                .andExpect(jsonPath("$.errors[*].field",
                        org.hamcrest.Matchers.hasItems("email", "password", "preferredCurrency", "firstName")));
    }

    @Test
    void passwordWithoutDigitIsRejected() throws Exception {
        Map<String, Object> body = registration(uniqueEmail());
        body.put("password", "OnlyLettersHere");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void malformedJsonIsBadRequestInProblemFormat() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()));
    }

    @Test
    void loginReturnsBearerTokenForValidCredentialsIgnoringEmailCase() throws Exception {
        String email = uniqueEmail();
        register(email);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email.toUpperCase(), "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresAt").value(notNullValue()))
                .andExpect(jsonPath("$.user.email").value(email))
                .andReturn();

        JsonNode body = body(result);
        assertThat(body.get("accessToken").asText()).matches("^[\\w-]+\\.[\\w-]+\\.[\\w-]+$");
    }

    @Test
    void wrongPasswordAndUnknownEmailFailIdentically() throws Exception {
        String email = uniqueEmail();
        register(email);

        MvcResult wrongPassword = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "Wr0ngPassword"))))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andReturn();
        MvcResult unknownEmail = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", uniqueEmail(), "password", PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(body(wrongPassword).get("detail")).isEqualTo(body(unknownEmail).get("detail"));
        assertThat(body(wrongPassword).get("detail").asText()).isEqualTo("Invalid email or password.");
    }

    @Test
    void loginWithMissingFieldsIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("email", "password")))
                .andExpect(jsonPath("$.detail", not("Invalid email or password.")));
    }
}
