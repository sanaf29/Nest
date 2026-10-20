package com.nest.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.nest.service.RecurringPausedEvent;
import com.nest.service.RecurringTransactionProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RecordApplicationEvents
class RecurringTransactionIT extends AbstractIntegrationTest {

    @Autowired
    private RecurringTransactionProcessor processor;

    @Autowired
    private ApplicationEvents events;

    @Autowired
    private java.time.Clock clock;

    /** The application's notion of today (UTC), not the test machine's local date. */
    private LocalDate today;
    private String token;
    private long chequing;
    private long housing;

    @BeforeEach
    void setUp() throws Exception {
        today = LocalDate.now(clock);
        token = registerAndLogin();
        chequing = openAccount(token, "Chequing", "CHECKING", "5000", null);
        housing = seededCategoryId("Housing");
    }

    private Map<String, Object> rule(long accountId, long categoryId, String amount, String frequency, LocalDate start) {
        Map<String, Object> body = new HashMap<>();
        body.put("accountId", accountId);
        body.put("categoryId", categoryId);
        body.put("type", "EXPENSE");
        body.put("amount", amount);
        body.put("description", "Rent");
        body.put("frequency", frequency);
        body.put("startDate", start.toString());
        return body;
    }

    private ResultActions create(String userToken, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post("/api/recurring-transactions").header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }

    private int occurrences(long ruleId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM transactions WHERE recurring_transaction_id = ?", Integer.class, ruleId);
    }

    @Test
    void pastStartDateIsCaughtUpImmediatelyAndRerunsNeverDuplicate() throws Exception {
        LocalDate start = today.minusWeeks(3);
        JsonNode created = body(create(token, rule(chequing, housing, "100.00", "WEEKLY", start)).andExpect(status().isCreated()).andReturn());
        long id = created.get("id").asLong();

        assertThat(occurrences(id)).isEqualTo(4); // start, +1w, +2w, +3w (= today)
        assertThat(created.get("nextOccurrence").asText()).isEqualTo(start.plusWeeks(4).toString());
        assertThat(created.get("upcoming")).hasSize(3);

        mockMvc.perform(post("/api/recurring-transactions/run").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.generated").value(0));
        processor.processDue(today);
        assertThat(occurrences(id)).isEqualTo(4);

        // Generated occurrences are ordinary expenses that move the balance: 5000 − 4 × 100.
        mockMvc.perform(get("/api/accounts/{id}", chequing).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.currentBalance").value(4600.00));
    }

    @Test
    void concurrentRunsGenerateEachOccurrenceOnce() throws Exception {
        long id = body(create(token, rule(chequing, housing, "10", "WEEKLY", today.plusDays(1))).andReturn()).get("id").asLong();
        LocalDate later = today.plusWeeks(6);

        ExecutorService pool = Executors.newFixedThreadPool(4);
        Callable<RecurringTransactionProcessor.Result> run = () -> processor.processDue(later);
        Future<?>[] futures = {pool.submit(run), pool.submit(run), pool.submit(run), pool.submit(run)};
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();

        assertThat(occurrences(id)).isEqualTo(6);
        Integer distinct = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT occurrence_date) FROM transactions WHERE recurring_transaction_id = ?", Integer.class, id);
        assertThat(distinct).isEqualTo(6);
    }

    @Test
    void databaseRejectsADuplicateOccurrence() throws Exception {
        long id = body(create(token, rule(chequing, housing, "10", "MONTHLY", today)).andReturn()).get("id").asLong();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO transactions (account_id, category_id, transaction_type, amount, transaction_date, "
                        + "recurring_transaction_id, occurrence_date, created_at, updated_at) "
                        + "VALUES (?, ?, 'EXPENSE', 10, ?, ?, ?, SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET())",
                chequing, housing, today, id, today))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void unusableAccountPausesTheRuleWithAReasonAndAnEvent() throws Exception {
        long id = body(create(token, rule(chequing, housing, "10", "MONTHLY", today.plusDays(5))).andReturn()).get("id").asLong();
        mockMvc.perform(delete("/api/accounts/{id}", chequing).header(HttpHeaders.AUTHORIZATION, bearer(token)));

        RecurringTransactionProcessor.Result result = processor.processDue(today.plusDays(5));

        assertThat(result.paused()).isGreaterThanOrEqualTo(1);
        assertThat(occurrences(id)).isZero();
        mockMvc.perform(get("/api/recurring-transactions/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.status").value("PAUSED"))
                .andExpect(jsonPath("$.pauseReason").value(org.hamcrest.Matchers.containsString("closed")));
        assertThat(events.stream(RecurringPausedEvent.class).filter(e -> e.recurringTransactionId() == id)).hasSize(1);
        // A paused rule can't be resumed while its account is still closed.
        mockMvc.perform(post("/api/recurring-transactions/{id}/resume", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void pauseResumeUpdateAndEnd() throws Exception {
        long id = body(create(token, rule(chequing, housing, "1650", "MONTHLY", today.plusDays(1))).andReturn()).get("id").asLong();

        mockMvc.perform(post("/api/recurring-transactions/{id}/pause", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.status").value("PAUSED"));
        processor.processDue(today.plusMonths(2));
        assertThat(occurrences(id)).isZero();

        mockMvc.perform(post("/api/recurring-transactions/{id}/resume", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        mockMvc.perform(put("/api/recurring-transactions/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("accountId", chequing, "categoryId", housing, "amount", "1700", "description", "Rent 2027"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(1700.00))
                .andExpect(jsonPath("$.description").value("Rent 2027"));

        mockMvc.perform(delete("/api/recurring-transactions/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());
        processor.processDue(today.plusYears(1));
        mockMvc.perform(get("/api/recurring-transactions/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.status").value("ENDED"))
                .andExpect(jsonPath("$.upcoming.length()").value(0));
    }

    @Test
    void priceIncreaseShowsUpAsAnInsight() throws Exception {
        long streaming = seededCategoryId("Subscriptions");
        Map<String, Object> body = rule(chequing, streaming, "15.99", "MONTHLY", today.minusMonths(1));
        body.put("description", "Streaming");
        long id = body(create(token, body).andReturn()).get("id").asLong(); // two occurrences at 15.99
        mockMvc.perform(put("/api/recurring-transactions/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("accountId", chequing, "categoryId", streaming, "amount", "17.99", "description", "Streaming"))));
        processor.processDue(today.plusMonths(1)); // next occurrence at 17.99

        mockMvc.perform(get("/api/insights").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.insights[?(@.code == 'RECURRING_EXPENSE_INCREASED')].message")
                        .value(org.hamcrest.Matchers.hasItem("Streaming went from 15.99 CAD to 17.99 CAD (+13%).")));
    }

    @Test
    void validationAndIsolation() throws Exception {
        Map<String, Object> income = rule(chequing, housing, "10", "MONTHLY", today);
        income.put("type", "INCOME");
        create(token, income).andExpect(status().isUnprocessableEntity()); // Housing is an expense category
        create(token, rule(chequing, housing, "10", "MONTHLY", today.minusYears(2))).andExpect(status().isUnprocessableEntity());
        create(token, rule(chequing, housing, "-1", "MONTHLY", today)).andExpect(status().isBadRequest());
        create(token, rule(chequing, housing, "10", "DAILY", today)).andExpect(status().isBadRequest());

        long id = body(create(token, rule(chequing, housing, "10", "MONTHLY", today.plusDays(1))).andReturn()).get("id").asLong();
        String other = registerAndLogin();
        mockMvc.perform(get("/api/recurring-transactions/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/recurring-transactions/{id}/pause", id).header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isNotFound());
        create(other, rule(chequing, housing, "10", "MONTHLY", today)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/recurring-transactions").header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(jsonPath("$.length()").value(0));
    }
}
