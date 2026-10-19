package com.nest.dto.response;

import java.time.Instant;

/** Public view of a user. It never includes the password hash. */
public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String preferredCurrency,
        Instant createdAt,
        Instant updatedAt
) {
}
