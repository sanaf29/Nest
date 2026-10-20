package com.nest.dto.response;

/** Outcome of processing due recurring transactions. */
public record RecurringRunResponse(int generated, int paused) {
}
