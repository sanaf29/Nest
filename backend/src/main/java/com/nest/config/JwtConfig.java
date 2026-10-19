package com.nest.config;

import com.nest.security.JwtProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Configures the HS256 signing key shared by token issuing ({@link JwtEncoder}) and validation
 * ({@link JwtDecoder}). Startup fails fast if the secret is missing or weak, so the application
 * never runs with an insecure default.
 */
@Configuration(proxyBeanMethods = false)
public class JwtConfig {

    private static final int MIN_KEY_BYTES = 32; // 256 bits, required for HS256

    @Bean
    public SecretKey jwtSigningKey(JwtProperties properties) {
        String secret = properties.secret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "NEST_JWT_SECRET is not set. Generate one with: openssl rand -base64 32");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret.strip());
        } catch (IllegalArgumentException notBase64) {
            throw new IllegalStateException("NEST_JWT_SECRET must be Base64-encoded.", notBase64);
        }
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException(
                    "NEST_JWT_SECRET must decode to at least %d bytes.".formatted(MIN_KEY_BYTES));
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSigningKey, JwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // Checks the signature, exp/nbf (with clock skew), and that the issuer is ours.
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        return decoder;
    }
}
