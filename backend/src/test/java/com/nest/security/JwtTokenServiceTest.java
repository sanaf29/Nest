package com.nest.security;

import com.nest.config.JwtConfig;
import com.nest.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Round-trips real tokens through the same encoder/decoder configuration the application uses. */
class JwtTokenServiceTest {

    private static final String SECRET = "bmVzdC10ZXN0LW9ubHktc2VjcmV0LWtleS0zMi1ieXRlcw==";
    private static final Duration TTL = Duration.ofMinutes(60);

    private final JwtConfig jwtConfig = new JwtConfig();
    private User user;

    @BeforeEach
    void setUp() {
        user = new User("Sanae", "Faghfouri", "sanae@example.com", "$2a$hash", "CAD");
        ReflectionTestUtils.setField(user, "id", 42L);
    }

    @Test
    void issuesTokenWhoseSubjectIsTheUserIdAndWhichTheDecoderAccepts() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        AccessToken token = tokenService(SECRET, Clock.fixed(now, ZoneOffset.UTC)).issue(user);
        Jwt decoded = decoder(SECRET).decode(token.value());

        assertThat(decoded.getSubject()).isEqualTo("42");
        assertThat(decoded.getClaimAsString(JwtTokenService.EMAIL_CLAIM)).isEqualTo("sanae@example.com");
        assertThat(decoded.getClaimAsString("iss")).isEqualTo("nest");
        assertThat(decoded.getExpiresAt()).isEqualTo(now.plus(TTL));
        assertThat(token.expiresAt()).isEqualTo(now.plus(TTL));
    }

    @Test
    void expiredTokenIsRejected() {
        Clock twoHoursAgo = Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC);
        AccessToken token = tokenService(SECRET, twoHoursAgo).issue(user);

        assertThatThrownBy(() -> decoder(SECRET).decode(token.value()))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String attackerSecret = "YW5vdGhlci1zZWNyZXQta2V5LXRoYXQtaXMtMzItYnl0ZXM=";
        AccessToken forged = tokenService(attackerSecret, Clock.systemUTC()).issue(user);

        assertThatThrownBy(() -> decoder(SECRET).decode(forged.value()))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() {
        JwtProperties otherIssuer = new JwtProperties(SECRET, "not-nest", TTL);
        AccessToken token = new JwtTokenService(
                jwtConfig.jwtEncoder(jwtConfig.jwtSigningKey(otherIssuer)), otherIssuer, Clock.systemUTC()).issue(user);

        assertThatThrownBy(() -> decoder(SECRET).decode(token.value()))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void startupFailsWhenSecretIsMissingTooShortOrNotBase64() {
        assertThatThrownBy(() -> jwtConfig.jwtSigningKey(new JwtProperties("", "nest", TTL)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> jwtConfig.jwtSigningKey(new JwtProperties("c2hvcnQ=", "nest", TTL)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> jwtConfig.jwtSigningKey(new JwtProperties("not base64!", "nest", TTL)))
                .isInstanceOf(IllegalStateException.class);
    }

    private JwtTokenService tokenService(String secret, Clock clock) {
        JwtProperties props = new JwtProperties(secret, "nest", TTL);
        return new JwtTokenService(jwtConfig.jwtEncoder(jwtConfig.jwtSigningKey(props)), props, clock);
    }

    private JwtDecoder decoder(String secret) {
        JwtProperties props = new JwtProperties(secret, "nest", TTL);
        return jwtConfig.jwtDecoder(jwtConfig.jwtSigningKey(props), props);
    }
}
