package com.example.gomoku.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AuditLogger {

    private static final Logger log = LoggerFactory.getLogger("AUDIT");

    public void loginSucceeded(String username, String ip) {
        log.info("event=login_success username={} ip={}", username, ip);
    }

    public void loginFailed(String username, String ip) {
        log.info("event=login_failure username={} ip={}", username, ip);
    }

    public void registered(String username, String ip) {
        log.info("event=register username={} ip={}", username, ip);
    }

    public void accountLocked(String username, String ip) {
        log.info("event=account_locked username={} ip={}", username, ip);
    }
}
