package com.nest.integration;

import com.nest.security.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserProfileIT extends AbstractIntegrationTest {

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    void currentUserCanReadOwnProfile() throws Exception {
        String email = uniqueEmail();
        register(email);
        String token = login(email, PASSWORD);

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void currentUserCanUpdateNameAndCurrency() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(put("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("firstName", "Sana", "lastName", "F", "preferredCurrency", "EUR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Sana"))
                .andExpect(jsonPath("$.preferredCurrency").value("EUR"))
                .andExpect(jsonPath("$.updatedAt").value(notNullValue()));

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.firstName").value("Sana"))
                .andExpect(jsonPath("$.preferredCurrency").value("EUR"));
    }

    @Test
    void invalidProfileUpdateIsRejected() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(put("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("firstName", " ", "lastName", "F", "preferredCurrency", "eur"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    void eachTokenOnlyEverResolvesToItsOwnUser() throws Exception {
        String aliceEmail = uniqueEmail();
        String bobEmail = uniqueEmail();
        register(aliceEmail);
        register(bobEmail);

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(login(aliceEmail, PASSWORD))))
                .andExpect(jsonPath("$.email").value(aliceEmail));
        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(login(bobEmail, PASSWORD))))
                .andExpect(jsonPath("$.email").value(bobEmail));
    }

    @Test
    void requestWithoutTokenIsUnauthorizedInProblemFormat() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.instance").value("/api/users/me"));
    }

    @Test
    void malformedTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void expiredTokenIsUnauthorized() throws Exception {
        Instant issuedAt = Instant.now().minus(Duration.ofHours(2));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject("1")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(Duration.ofMinutes(15)))
                .build();
        String expired = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(expired)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void swaggerDocumentationIsPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Nest API"))
                .andExpect(jsonPath("$.paths['/api/auth/login']").exists());
    }
}
