package com.nest.model;

import com.nest.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReminderTest {

    private static final Instant NOW = Instant.parse("2026-10-19T12:00:00Z");
    private final User owner = new User("A", "B", "a@example.com", "$2a$hash", "CAD");

    private static Reminder.Details timed(Instant dueAt, int offset, String location) {
        return new Reminder.Details("Move the car", null, dueAt, offset, location, null, null, null);
    }

    @Test
    void parkingFiresOffsetMinutesBeforeExpiry() {
        Instant expiry = NOW.plus(2, ChronoUnit.HOURS);
        Reminder parking = new Reminder(owner, ReminderType.PARKING, timed(expiry, 15, "Level 2, B14"), NOW);

        assertThat(parking.remindAt()).isEqualTo(expiry.minus(15, ChronoUnit.MINUTES));
        assertThat(parking.isDue(expiry.minus(16, ChronoUnit.MINUTES))).isFalse();
        assertThat(parking.isDue(expiry.minus(15, ChronoUnit.MINUTES))).isTrue();
        parking.markSent(NOW);
        assertThat(parking.isDue(expiry)).isFalse();
    }

    @Test
    void typeSpecificRequirements() {
        Instant later = NOW.plus(1, ChronoUnit.DAYS);
        assertThatThrownBy(() -> new Reminder(owner, ReminderType.PARKING, timed(later, 0, " "), NOW))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("parked");
        assertThatThrownBy(() -> new Reminder(owner, ReminderType.BILL_PAYMENT, timed(null, 0, null), NOW))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new Reminder(owner, ReminderType.BILL_PAYMENT, timed(NOW.minusSeconds(60), 0, null), NOW))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("future");
        assertThatThrownBy(() -> new Reminder(owner, ReminderType.SAVINGS_GOAL_DEADLINE, timed(later, 0, null), NOW))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new Reminder(owner, ReminderType.BUDGET_THRESHOLD,
                new Reminder.Details("Budget", null, null, 0, null, null, null, 250), NOW))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new Reminder(owner, ReminderType.BUDGET_THRESHOLD,
                new Reminder.Details("Budget", null, later, 0, null, null, null, 80), NOW))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new Reminder(owner, ReminderType.CUSTOM, timed(later, Reminder.MAX_OFFSET_MINUTES + 1, null), NOW))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void budgetThresholdsAreNeverTimed() {
        Reminder alert = new Reminder(owner, ReminderType.BUDGET_THRESHOLD,
                new Reminder.Details("Budget at 80%", null, null, 0, null, null, null, 80), NOW);

        assertThat(alert.remindAt()).isNull();
        assertThat(alert.isDue(NOW.plus(365, ChronoUnit.DAYS))).isFalse();
    }

    @Test
    void editingReschedulesASentReminder() {
        Reminder bill = new Reminder(owner, ReminderType.BILL_PAYMENT, timed(NOW.plus(1, ChronoUnit.DAYS), 0, null), NOW);
        bill.markSent(NOW);

        bill.update(timed(NOW.plus(30, ChronoUnit.DAYS), 60, null), NOW);

        assertThat(bill.getStatus()).isEqualTo(ReminderStatus.SCHEDULED);
        assertThat(bill.getSentAt()).isNull();
    }
}
