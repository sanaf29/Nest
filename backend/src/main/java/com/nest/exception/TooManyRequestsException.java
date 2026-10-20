package com.nest.exception;

import java.time.Duration;

/** The caller has made too many attempts; they may retry after {@link #retryAfter()}. */
public class TooManyRequestsException extends NestException {

    private final Duration retryAfter;

    public TooManyRequestsException(String message, Duration retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
