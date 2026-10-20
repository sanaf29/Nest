package com.nest.security;

import com.nest.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptLimiterTest {

    /** A clock the test can move forward. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-19T12:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final LoginAttemptLimiter limiter = new LoginAttemptLimiter(new LoginAttemptLimiter.Settings(3, Duration.ofMinutes(15)), clock);

    @Test
    void locksOutAfterTheLimitThenRecoversWhenFailuresAge() {
        for (int i = 0; i < 3; i++) {
            limiter.checkAllowed("A@x.com");
            limiter.recordFailure("a@x.com ");
            clock.advance(Duration.ofMinutes(1));
        }

        assertThatThrownBy(() -> limiter.checkAllowed("a@x.com"))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageContaining("12 minutes")
                .satisfies(e -> assertThat(((TooManyRequestsException) e).retryAfter()).isEqualTo(Duration.ofMinutes(12)));
        assertThatCode(() -> limiter.checkAllowed("other@x.com")).doesNotThrowAnyException();

        clock.advance(Duration.ofMinutes(12)); // the first failure leaves the window
        assertThatCode(() -> limiter.checkAllowed("a@x.com")).doesNotThrowAnyException();
    }

    @Test
    void successClearsHistory() {
        limiter.recordFailure("a@x.com");
        limiter.recordFailure("a@x.com");
        limiter.recordSuccess("a@x.com");
        limiter.recordFailure("a@x.com");
        limiter.recordFailure("a@x.com");

        assertThatCode(() -> limiter.checkAllowed("a@x.com")).doesNotThrowAnyException();
    }
}
