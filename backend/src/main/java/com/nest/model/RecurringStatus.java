package com.nest.model;

/** ACTIVE rules generate occurrences; PAUSED rules wait to be resumed; ENDED rules never run again. */
public enum RecurringStatus {
    ACTIVE,
    PAUSED,
    ENDED
}
