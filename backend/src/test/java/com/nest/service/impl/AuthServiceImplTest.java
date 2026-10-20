package com.nest.service.impl;

import com.nest.dto.request.LoginRequest;
import com.nest.dto.request.RegisterRequest;
import com.nest.dto.response.AuthResponse;
import com.nest.dto.response.UserResponse;
import com.nest.exception.ConflictException;
import com.nest.exception.InvalidCredentialsException;
import com.nest.mapper.UserMapper;
import com.nest.model.User;
import com.nest.repository.UserRepository;
import com.nest.security.AccessToken;
import com.nest.security.LoginAttemptLimiter;
import com.nest.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String DUMMY_HASH = "$2a$dummy";

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenService tokenService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode("nest-timing-equalizer")).thenReturn(DUMMY_HASH);
        authService = new AuthServiceImpl(userRepository, passwordEncoder, tokenService, new UserMapper(),
                new LoginAttemptLimiter(new LoginAttemptLimiter.Settings(5, java.time.Duration.ofMinutes(15)), java.time.Clock.systemUTC()));
    }

    @Nested
    class Register {

        private final RegisterRequest request =
                new RegisterRequest("Sanae", "Faghfouri", "Sanae@Example.com", "Str0ngPass", "CAD");

        @Test
        void storesNormalisedEmailAndHashedPasswordOnly() {
            when(userRepository.existsByEmail("sanae@example.com")).thenReturn(false);
            when(passwordEncoder.encode("Str0ngPass")).thenReturn("$2a$hashed");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UserResponse response = authService.register(request);

            ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(saved.capture());
            User user = saved.getValue();
            assertThat(user.getEmail()).isEqualTo("sanae@example.com");
            // The entity checks passwords against its stored value; it matches only if that value is the hash.
            when(passwordEncoder.matches("Str0ngPass", "$2a$hashed")).thenReturn(true);
            assertThat(user.passwordMatches("Str0ngPass", passwordEncoder)).isTrue();
            assertThat(response.email()).isEqualTo("sanae@example.com");
            assertThat(response.preferredCurrency()).isEqualTo("CAD");
        }

        @Test
        void rejectsDuplicateEmailRegardlessOfCase() {
            when(userRepository.existsByEmail("sanae@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(ConflictException.class);
            verify(userRepository, never()).save(any());
            verify(passwordEncoder, never()).encode("Str0ngPass");
        }
    }

    @Nested
    class Login {

        private final User user = new User("Sanae", "Faghfouri", "sanae@example.com", "$2a$stored", "CAD");

        @Test
        void issuesTokenForValidCredentials() {
            Instant expiry = Instant.parse("2026-10-19T15:00:00Z");
            when(userRepository.findByEmail("sanae@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("Str0ngPass", "$2a$stored")).thenReturn(true);
            when(tokenService.issue(user)).thenReturn(new AccessToken("jwt-value", expiry));

            AuthResponse response = authService.login(new LoginRequest(" SANAE@example.com", "Str0ngPass"));

            assertThat(response.accessToken()).isEqualTo("jwt-value");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.expiresAt()).isEqualTo(expiry);
            assertThat(response.user().email()).isEqualTo("sanae@example.com");
        }

        @Test
        void rejectsWrongPasswordWithoutIssuingToken() {
            when(userRepository.findByEmail("sanae@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong-pass1", "$2a$stored")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(new LoginRequest("sanae@example.com", "wrong-pass1")))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Invalid email or password.");
            verify(tokenService, never()).issue(any());
        }

        @Test
        void unknownEmailFailsIdenticallyAndStillPerformsAHashCheck() {
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(new LoginRequest("ghost@example.com", "whatever1")))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Invalid email or password.");
            verify(passwordEncoder).matches(eq("whatever1"), eq(DUMMY_HASH));
            verify(tokenService, never()).issue(any());
        }

        @Test
        void neverConsultsTokenServiceBeforePasswordIsVerified() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(new LoginRequest("sanae@example.com", "x1234567")))
                    .isInstanceOf(InvalidCredentialsException.class);
            verify(tokenService, never()).issue(any());
        }
    }
}
