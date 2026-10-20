package com.nest.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.nest.service.RecurringTransactionProcessor;
import com.nest.service.ReminderDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReminderIT extends AbstractIntegrationTest {

    @Autowired
    private ReminderDispatcher dispatcher;

    @Autowired
    private RecurringTransactionProcessor recurringProcessor;

    @Autowired
    private Clock clock;

    private String token;
    private Instant now;

    @BeforeEach
    void setUp() throws Exception {
        token = registerAndLogin();
        now = clock.instant();
    }

    private ResultActions createReminder(String userToken, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post("/api/reminders").header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }

    private JsonNode notifications(String userToken) throws Exception {
        return body(mockMvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk()).andReturn()).get("content");
    }

    private long unread(String userToken) throws Exception {
        return body(mockMvc.perform(get("/api/notifications/unread-count").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andReturn()).get("count").asLong();
    }

    @Test
    void parkingReminderNotifiesOnceAtExpiryMinusOffset() throws Exception {
        Instant expiry = now.plus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        JsonNode created = body(createReminder(token, Map.of("type", "PARKING", "title", "Move the car",
                "dueAt", expiry.toString(), "offsetMinutes", 15, "location", "Level 2, spot B14"))
                .andExpect(status().isCreated()).andReturn());
        assertThat(Instant.parse(created.get("remindAt").asText())).isEqualTo(expiry.minus(15, ChronoUnit.MINUTES));

        dispatcher.dispatchTimedReminders(expiry.minus(20, ChronoUnit.MINUTES));
        assertThat(unread(token)).isZero();

        dispatcher.runAll(expiry.minus(15, ChronoUnit.MINUTES));
        dispatcher.runAll(expiry.minus(10, ChronoUnit.MINUTES)); // rerun: no duplicate
        dispatcher.runAll(expiry);

        JsonNode list = notifications(token);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).get("title").asText()).isEqualTo("Parking expires: Move the car");
        assertThat(list.get(0).get("message").asText()).contains("Level 2, spot B14");
        mockMvc.perform(get("/api/reminders/{id}", created.get("id").asLong()).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    void invalidRemindersAreRejected() throws Exception {
        createReminder(token, Map.of("type", "PARKING", "title", "Car", "dueAt", now.plusSeconds(3600).toString()))
                .andExpect(status().isUnprocessableEntity());
        createReminder(token, Map.of("type", "BILL_PAYMENT", "title", "Hydro", "dueAt", now.minusSeconds(3600).toString()))
                .andExpect(status().isUnprocessableEntity());
        createReminder(token, Map.of("type", "BUDGET_THRESHOLD", "title", "Budget")).andExpect(status().isUnprocessableEntity());
        createReminder(token, Map.of("type", "TELEPATHY", "title", "x")).andExpect(status().isBadRequest());
    }

    @Test
    void budgetThresholdNotifiesOncePerMonth() throws Exception {
        YearMonth month = YearMonth.from(now.atZone(ZoneOffset.UTC));
        long chequing = openAccount(token, "Chequing", "CHECKING", "0", null);
        long dining = seededCategoryId("Dining Out");
        mockMvc.perform(post("/api/budgets").header(HttpHeaders.AUTHORIZATION, bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("year", month.getYear(), "month", month.getMonthValue(), "plannedIncome", "1000",
                        "savingsStrategy", "PERCENTAGE", "savingsValue", 0,
                        "allocations", List.of(Map.of("categoryId", dining, "limit", "100"))))))
                .andExpect(status().isCreated());
        createReminder(token, Map.of("type", "BUDGET_THRESHOLD", "title", "Dining at 80%", "categoryId", dining, "thresholdPercent", 80))
                .andExpect(status().isCreated());

        record(token, chequing, dining, "EXPENSE", "70.00", month.atDay(1).toString());
        dispatcher.evaluateBudgetThresholds(now);
        assertThat(unread(token)).isZero();

        record(token, chequing, dining, "EXPENSE", "15.00", month.atDay(1).toString()); // 85 %
        dispatcher.evaluateBudgetThresholds(now);
        record(token, chequing, dining, "EXPENSE", "50.00", month.atDay(1).toString()); // 135 %: same month, no repeat
        dispatcher.evaluateBudgetThresholds(now);

        JsonNode list = notifications(token);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).get("title").asText()).isEqualTo("Dining Out has reached 80% of its budget");
        assertThat(list.get(0).get("category").asText()).isEqualTo("BUDGET");
    }

    @Test
    void recurringBillsSendADueSoonNoticeAndPausesAreReported() throws Exception {
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        long chequing = openAccount(token, "Chequing", "CHECKING", "0", null);
        Map<String, Object> rule = new HashMap<>(Map.of("accountId", chequing, "categoryId", seededCategoryId("Utilities"),
                "type", "EXPENSE", "amount", "80", "description", "Hydro", "frequency", "MONTHLY",
                "startDate", today.plusDays(3).toString(), "remindDaysBefore", 3));
        long ruleId = body(mockMvc.perform(post("/api/recurring-transactions").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(json(rule))).andReturn()).get("id").asLong();

        dispatcher.notifyUpcomingRecurring(now);
        dispatcher.notifyUpcomingRecurring(now.plus(1, ChronoUnit.DAYS));
        assertThat(notifications(token)).hasSize(1);
        assertThat(notifications(token).get(0).get("title").asText()).startsWith("Hydro is due");

        mockMvc.perform(delete("/api/accounts/{id}", chequing).header(HttpHeaders.AUTHORIZATION, bearer(token)));
        recurringProcessor.processDue(today.plusDays(3));
        recurringProcessor.processDue(today.plusDays(4));

        JsonNode list = notifications(token);
        assertThat(list).hasSize(2);
        assertThat(list.get(0).get("title").asText()).isEqualTo("'Hydro' was paused");
        assertThat(ruleId).isPositive();
    }

    @Test
    void reachingAGoalNotifies() throws Exception {
        long goal = body(mockMvc.perform(post("/api/savings-goals").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("name", "Bike", "targetAmount", "100")))).andReturn())
                .get("id").asLong();
        mockMvc.perform(post("/api/savings-goals/{id}/contributions", goal).header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("amount", "100"))));

        mockMvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.content[0].title").value(containsString("Goal achieved: Bike")))
                .andExpect(jsonPath("$.content[0].link").value("/goals"));
    }

    @Test
    void readStateAndIsolation() throws Exception {
        for (int i = 0; i < 2; i++) {
            createReminder(token, Map.of("type", "CUSTOM", "title", "Call bank " + i, "dueAt", now.plusSeconds(60).toString()));
        }
        dispatcher.dispatchTimedReminders(now.plusSeconds(120));
        assertThat(unread(token)).isEqualTo(2);

        long first = notifications(token).get(0).get("id").asLong();
        String other = registerAndLogin();
        mockMvc.perform(post("/api/notifications/{id}/read", first).header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isNotFound());
        assertThat(notifications(other)).isEmpty();

        mockMvc.perform(post("/api/notifications/{id}/read", first).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.read").value(true));
        assertThat(unread(token)).isEqualTo(1);
        mockMvc.perform(post("/api/notifications/read-all").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.updated").value(1));
        assertThat(unread(token)).isZero();
        mockMvc.perform(get("/api/notifications?unreadOnly=true").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void dismissAndDeleteAreOwnerOnly() throws Exception {
        long id = body(createReminder(token, Map.of("type", "SUBSCRIPTION_RENEWAL", "title", "Gym renews",
                "dueAt", now.plus(5, ChronoUnit.DAYS).toString(), "offsetMinutes", 1440)).andReturn()).get("id").asLong();
        String other = registerAndLogin();

        mockMvc.perform(post("/api/reminders/{id}/dismiss", id).header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/reminders/{id}/dismiss", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.status").value("DISMISSED"));
        dispatcher.dispatchTimedReminders(now.plus(10, ChronoUnit.DAYS));
        assertThat(unread(token)).isZero();
        mockMvc.perform(delete("/api/reminders/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());
    }
}
