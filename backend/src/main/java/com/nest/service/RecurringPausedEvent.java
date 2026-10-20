package com.nest.service;

import java.time.LocalDate;

/**
 * Published when the processor pauses a rule because its account or category can no longer be
 * used. Listeners (notifications) react without the processor knowing about them.
 */
public record RecurringPausedEvent(long userId, long recurringTransactionId, String description, String reason, LocalDate date) {
}
