package com.nest.model;

/** SCHEDULED reminders are waiting (or, for budget thresholds, being watched); SENT fired; DISMISSED was turned off. */
public enum ReminderStatus {
    SCHEDULED,
    SENT,
    DISMISSED
}
