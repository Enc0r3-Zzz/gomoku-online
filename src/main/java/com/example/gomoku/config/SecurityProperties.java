package com.example.gomoku.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "gomoku.security")
public record SecurityProperties(
        int loginMaxFailures,
        Duration loginLockDuration,
        int registerMaxPerWindow,
        Duration registerWindow
) {
}
