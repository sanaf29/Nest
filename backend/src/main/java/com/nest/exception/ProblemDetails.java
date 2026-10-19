package com.nest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.time.Instant;
import java.util.List;

/**
 * Builds the RFC 7807 error body used by every error path: controllers, validation, and the
 * security filter chain. This keeps the format identical wherever an error originates.
 */
public final class ProblemDetails {

    public static final String TIMESTAMP = "timestamp";
    public static final String ERRORS = "errors";

    private ProblemDetails() {
    }

    public static ProblemDetail of(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty(TIMESTAMP, Instant.now());
        return problem;
    }

    public static ProblemDetail of(HttpStatus status, String title, String detail, String path) {
        ProblemDetail problem = of(status, title, detail);
        problem.setInstance(URI.create(path));
        return problem;
    }

    public static void addFieldErrors(ProblemDetail problem, List<FieldError> errors) {
        problem.setProperty(ERRORS, errors);
    }

    /** One invalid input field. */
    public record FieldError(String field, String message) {
    }
}
