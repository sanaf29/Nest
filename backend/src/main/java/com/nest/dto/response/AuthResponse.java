package com.nest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record AuthResponse(
        @Schema(description = "JWT to send as 'Authorization: Bearer <token>'") String accessToken,
        @Schema(example = "Bearer") String tokenType,
        Instant expiresAt,
        UserResponse user
) {
    public static AuthResponse bearer(String accessToken, Instant expiresAt, UserResponse user) {
        return new AuthResponse(accessToken, "Bearer", expiresAt, user);
    }
}
