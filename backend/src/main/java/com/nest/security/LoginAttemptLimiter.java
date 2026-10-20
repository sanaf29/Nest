package com.nest.security;

import com.nest.exception.TooManyRequestsException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Slows down password guessing: after {@code maxFailures} failed logins for one email within
 * {@code window}, further attempts are refused (429) until the oldest failure leaves the window.
 * A successful login clears the history.
 *
 * <p>In-memory, so it applies per application instance. For several instances, the same
 * contract would move to a shared store such as Redis.
 */
@Component
public class LoginAttemptLimiter {

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();
    private final Settings settings;
    private final Clock clock;

    @ConfigurationProperties(prefix = "nest.security.login-throttle")
    public record Settings(int maxFailures, Duration window) {
        public Settings {
            if (maxFailures <= 0) maxFailures = 5;
            if (window == null) window = Duration.ofMinutes(15);
        }
    }

    public LoginAttemptLimiter(Settings settings, Clock clock) {
        this.settings = settings;
        this.clock = clock;
    }

    /** @throws TooManyRequestsException if this email is currently locked out */
    public void checkAllowed(String email) {
        Deque<Instant> history = failures.get(key(email));
        if (history == null) {
            return;
        }
        synchronized (history) {
            Instant now = clock.instant();
            prune(history, now);
            if (history.size() >= settings.maxFailures()) {
                Duration retryAfter = Duration.between(now, history.peekFirst().plus(settings.window()));
                throw new TooManyRequestsException("Too many failed login attempts. Try again in %d minute%s."
                        .formatted(minutes(retryAfter), minutes(retryAfter) == 1 ? "" : "s"), retryAfter);
            }
        }
    }

    public void recordFailure(String email) {
        Deque<Instant> history = failures.computeIfAbsent(key(email), k -> new ArrayDeque<>());
        synchronized (history) {
            Instant now = clock.instant();
            prune(history, now);
            history.addLast(now);
        }
    }

    public void recordSuccess(String email) {
        failures.remove(key(email));
    }

    private void prune(Deque<Instant> history, Instant now) {
        Instant cutoff = now.minus(settings.window());
        while (!history.isEmpty() && !history.peekFirst().isAfter(cutoff)) {
            history.removeFirst();
        }
    }

    private static long minutes(Duration d) {
        return Math.max(1, (d.getSeconds() + 59) / 60);
    }

    private static String key(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }
}
