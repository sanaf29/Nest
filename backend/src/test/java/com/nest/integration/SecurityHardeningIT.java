package com.nest.integration;

import com.nest.model.FinancialAccount;
import com.nest.repository.FinancialAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(OutputCaptureExtension.class)
class SecurityHardeningIT extends AbstractIntegrationTest {

    @Autowired
    private FinancialAccountRepository accountRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void repeatedFailedLoginsAreThrottled() throws Exception {
        String email = uniqueEmail();
        register(email);
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("email", email, "password", "Wr0ng-guess" + i))))
                    .andExpect(status().isUnauthorized());
        }

        // Even the right password is refused while locked out, so guessing can't continue.
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith("Too many failed login attempts")));
        // Other accounts are unaffected.
        String other = uniqueEmail();
        register(other);
        login(other, PASSWORD);
    }

    @Test
    void passwordChangeRequiresTheCurrentPassword() throws Exception {
        String email = uniqueEmail();
        register(email);
        String token = login(email, PASSWORD);

        mockMvc.perform(put("/api/users/me/password").header(HttpHeaders.AUTHORIZATION, bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", "N0t-my-password", "newPassword", "N3wPassword"))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/users/me/password").header(HttpHeaders.AUTHORIZATION, bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", PASSWORD, "newPassword", "short"))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/users/me/password").header(HttpHeaders.AUTHORIZATION, bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", PASSWORD, "newPassword", PASSWORD))))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(put("/api/users/me/password").header(HttpHeaders.AUTHORIZATION, bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", PASSWORD, "newPassword", "N3wPassword"))))
                .andExpect(status().isNoContent());

        login(email, "N3wPassword");
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiResponsesCarrySecurityHeaders() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
        // Swagger UI still loads (no API CSP on documentation pages).
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(header().doesNotExist("Content-Security-Policy"));
    }

    @Test
    void concurrentEditsOfTheSameRowAreDetectedNotLost() throws Exception {
        String token = registerAndLogin();
        long id = openAccount(token, "Shared", "CHECKING", "100", null);
        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        TransactionTemplate inner = new TransactionTemplate(transactionManager);
        inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> outer.executeWithoutResult(status -> {
            FinancialAccount mine = accountRepository.findById(id).orElseThrow();   // reads version 0
            inner.executeWithoutResult(s -> {                                       // someone else saves first
                FinancialAccount theirs = accountRepository.findById(id).orElseThrow();
                theirs.rename("Renamed elsewhere");
            });
            mine.correctOpeningBalance(new BigDecimal("999"));                     // stale write
            accountRepository.flush();
        })).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        FinancialAccount stored = accountRepository.findById(id).orElseThrow();
        assertThat(stored.getName()).isEqualTo("Renamed elsewhere");
        assertThat(stored.openingBalance().amount()).isEqualByComparingTo("100"); // the stale write did not win
    }

    @Test
    void passwordsAndTokensNeverReachTheLogs(CapturedOutput output) throws Exception {
        String email = uniqueEmail();
        String secret = "Un1que-Secret-" + System.nanoTime();
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("firstName", "L", "lastName", "G", "email", email, "password", secret, "preferredCurrency", "CAD"))));
        String token = login(email, secret);
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", secret + "x"))));

        assertThat(output.getAll()).doesNotContain(secret).doesNotContain(token);
    }
}
