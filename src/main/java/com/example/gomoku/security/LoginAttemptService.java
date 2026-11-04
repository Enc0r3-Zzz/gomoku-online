package com.example.gomoku.security;

import com.example.gomoku.config.SecurityProperties;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败计数与临时锁定（单实例内存实现）。
 * key = username|ip，成功登录清零，锁定到期自动失效。
 */
@Service
public class LoginAttemptService {

    private final int maxFailures;
    private final Duration lockDuration;
    private final Clock clock;
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(SecurityProperties properties, Clock clock) {
        this.maxFailures = properties.loginMaxFailures();
        this.lockDuration = properties.loginLockDuration();
        this.clock = clock;
    }

    public boolean isBlocked(String username, String ip) {
        if (isBlank(username)) {
            return false;
        }
        String key = key(username, ip);
        Attempt attempt = attempts.get(key);
        if (attempt == null || attempt.lockedUntil == null) {
            return false;
        }
        if (attempt.lockedUntil.isAfter(clock.instant())) {
            return true;
        }
        attempts.remove(key, attempt);
        return false;
    }

    public void loginFailed(String username, String ip) {
        if (isBlank(username)) {
            return;
        }
        attempts.compute(key(username, ip), (ignored, attempt) -> {
            Attempt current = attempt != null ? attempt : new Attempt();
            current.failures++;
            if (current.failures >= maxFailures) {
                current.lockedUntil = clock.instant().plus(lockDuration);
            }
            return current;
        });
    }

    public void loginSucceeded(String username, String ip) {
        if (isBlank(username)) {
            return;
        }
        attempts.remove(key(username, ip));
    }

    public long remainingLockSeconds(String username, String ip) {
        if (isBlank(username)) {
            return 0;
        }
        Attempt attempt = attempts.get(key(username, ip));
        if (attempt == null || attempt.lockedUntil == null) {
            return 0;
        }
        long seconds = Duration.between(clock.instant(), attempt.lockedUntil).getSeconds();
        return Math.max(0, seconds);
    }

    public void clearAll() {
        attempts.clear();
    }

    private static String key(String username, String ip) {
        return username.trim().toLowerCase(Locale.ROOT) + "|" + (ip == null ? "" : ip);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static final class Attempt {
        private int failures;
        private Instant lockedUntil;
    }
}
