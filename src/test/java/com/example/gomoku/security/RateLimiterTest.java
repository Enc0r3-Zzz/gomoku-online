package com.example.gomoku.security;

import com.example.gomoku.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    private MutableClock clock;
    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        clock = MutableClock.at("2026-01-01T00:00:00Z");
        rateLimiter = new RateLimiter(clock);
    }

    @Test
    void allowsUpToLimitInsideWindow() {
        assertThat(rateLimiter.tryAcquire("k", 2, Duration.ofMinutes(1))).isTrue();
        assertThat(rateLimiter.tryAcquire("k", 2, Duration.ofMinutes(1))).isTrue();
        assertThat(rateLimiter.tryAcquire("k", 2, Duration.ofMinutes(1))).isFalse();
    }

    @Test
    void windowSlidesAfterExpiry() {
        assertThat(rateLimiter.tryAcquire("k", 1, Duration.ofMinutes(1))).isTrue();
        assertThat(rateLimiter.tryAcquire("k", 1, Duration.ofMinutes(1))).isFalse();

        clock.advance(Duration.ofSeconds(61));

        assertThat(rateLimiter.tryAcquire("k", 1, Duration.ofMinutes(1))).isTrue();
    }

    @Test
    void keysAreIndependent() {
        assertThat(rateLimiter.tryAcquire("a", 1, Duration.ofMinutes(1))).isTrue();
        assertThat(rateLimiter.tryAcquire("b", 1, Duration.ofMinutes(1))).isTrue();
        assertThat(rateLimiter.tryAcquire("a", 1, Duration.ofMinutes(1))).isFalse();
    }
}
