package com.example.gomoku.security;

import com.example.gomoku.config.SecurityProperties;
import com.example.gomoku.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    private MutableClock clock;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        clock = MutableClock.at("2026-01-01T00:00:00Z");
        service = new LoginAttemptService(
                new SecurityProperties(5, Duration.ofMinutes(15), 5, Duration.ofMinutes(10)),
                clock);
    }

    @Test
    void blocksAfterFiveFailures() {
        for (int i = 0; i < 5; i++) {
            assertThat(service.isBlocked("alice", "1.1.1.1")).isFalse();
            service.loginFailed("alice", "1.1.1.1");
        }
        assertThat(service.isBlocked("alice", "1.1.1.1")).isTrue();
    }

    @Test
    void successfulLoginClearsFailures() {
        for (int i = 0; i < 4; i++) {
            service.loginFailed("alice", "1.1.1.1");
        }
        service.loginSucceeded("alice", "1.1.1.1");

        for (int i = 0; i < 4; i++) {
            service.loginFailed("alice", "1.1.1.1");
        }

        assertThat(service.isBlocked("alice", "1.1.1.1")).isFalse();
    }

    @Test
    void lockExpiresAfterDuration() {
        for (int i = 0; i < 5; i++) {
            service.loginFailed("alice", "1.1.1.1");
        }
        assertThat(service.isBlocked("alice", "1.1.1.1")).isTrue();

        clock.advance(Duration.ofMinutes(16));

        assertThat(service.isBlocked("alice", "1.1.1.1")).isFalse();
    }

    @Test
    void lockIsScopedToUsernameAndIp() {
        for (int i = 0; i < 5; i++) {
            service.loginFailed("alice", "1.1.1.1");
        }

        assertThat(service.isBlocked("alice", "1.1.1.1")).isTrue();
        assertThat(service.isBlocked("alice", "2.2.2.2")).isFalse();
        assertThat(service.isBlocked("bob", "1.1.1.1")).isFalse();
    }

    @Test
    void reportsRemainingLockSeconds() {
        for (int i = 0; i < 5; i++) {
            service.loginFailed("alice", "1.1.1.1");
        }

        assertThat(service.remainingLockSeconds("alice", "1.1.1.1")).isBetween(890L, 900L);
    }
}
