package com.example.gomoku.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WsSessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(WsSessionRegistry.class);

    private final ObjectMapper objectMapper;
    private final Map<Long, Entry> sessions = new ConcurrentHashMap<>();

    public WsSessionRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(Long userId, WebSocketSession session) {
        WebSocketSession safe = new ConcurrentWebSocketSessionDecorator(session, 5000, 64 * 1024);
        Entry previous = sessions.put(userId, new Entry(session, safe));
        if (previous != null && previous.safe().isOpen()) {
            try {
                previous.safe().close(new CloseStatus(4001, "REPLACED"));
            } catch (IOException ex) {
                log.debug("failed to close previous session for user {}", userId, ex);
            }
        }
    }

    public void remove(Long userId, WebSocketSession session) {
        sessions.computeIfPresent(userId, (id, entry) -> entry.original() == session ? null : entry);
    }

    public boolean isOnline(Long userId) {
        Entry entry = sessions.get(userId);
        return entry != null && entry.safe().isOpen();
    }

    public void send(Long userId, String op, Object data) {
        Entry entry = sessions.get(userId);
        if (entry == null || !entry.safe().isOpen()) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(Map.of("op", op, "data", data));
            entry.safe().sendMessage(new TextMessage(payload));
        } catch (IOException | IllegalStateException ex) {
            log.debug("failed to send {} to user {}", op, userId, ex);
            sessions.remove(userId, entry);
        }
    }

    private record Entry(WebSocketSession original, WebSocketSession safe) {
    }
}
