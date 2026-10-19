package com.nest.security;

import java.time.Instant;

/** An issued bearer token and its expiry. */
public record AccessToken(String value, Instant expiresAt) {

    @Override
    public String toString() {
        return "AccessToken[value=***, expiresAt=%s]".formatted(expiresAt);
    }
}
