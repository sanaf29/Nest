package com.nest.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT settings bound from {@code nest.security.jwt.*}.
 *
 * @param secret         Base64-encoded HMAC key (≥ 256 bits), supplied by NEST_JWT_SECRET
 * @param issuer         value of the {@code iss} claim, checked on every request
 * @param accessTokenTtl how long an access token remains valid
 */
@ConfigurationProperties(prefix = "nest.security.jwt")
public record JwtProperties(String secret, String issuer, Duration accessTokenTtl) {
}
