package com.nest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** The password policy is not re-applied at login, to avoid hinting at the rules for existing accounts. */
public record LoginRequest(
        @NotBlank @Schema(example = "sanae@example.com") String email,
        @NotBlank @Schema(example = "Str0ngPassw0rd") String password
) {
    @Override
    public String toString() {
        return "LoginRequest[email=%s, password=***]".formatted(email);
    }
}
