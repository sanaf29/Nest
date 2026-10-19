package com.nest.exception;

/**
 * Base type for all expected, domain-level failures in Nest.
 *
 * <p>Subclasses describe <em>what</em> went wrong in business terms; the mapping to an
 * HTTP status lives in {@link GlobalExceptionHandler}, so the domain stays free of web concerns.
 */
public abstract class NestException extends RuntimeException {

    protected NestException(String message) {
        super(message);
    }
}
