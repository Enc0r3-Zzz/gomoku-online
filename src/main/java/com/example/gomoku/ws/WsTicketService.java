package com.example.gomoku.ws;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 短时 WebSocket 连接票据。浏览器 Cookie 认证之外的第二条通路，
 * 也便于测试客户端与非浏览器客户端连接。
 */
@Service
public class WsTicketService {

    private static final Duration TTL = Duration.ofMinutes(5);

    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String issue(Long userId) {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tickets.put(token, new Ticket(userId, Instant.now().plus(TTL)));
        return token;
    }

    public Long validate(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Ticket ticket = tickets.get(token);
        if (ticket == null) {
            return null;
        }
        if (ticket.expiresAt().isBefore(Instant.now())) {
            tickets.remove(token);
            return null;
        }
        return ticket.userId();
    }

    public void revoke(String token) {
        if (token != null) {
            tickets.remove(token);
        }
    }

    private record Ticket(Long userId, Instant expiresAt) {
    }
}
