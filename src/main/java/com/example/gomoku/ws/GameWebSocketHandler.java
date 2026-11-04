package com.example.gomoku.ws;

import com.example.gomoku.common.ApiException;
import com.example.gomoku.common.ErrorCode;
import com.example.gomoku.game.GameService;
import com.example.gomoku.match.MatchmakingService;
import com.example.gomoku.user.User;
import com.example.gomoku.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(GameWebSocketHandler.class);
    private static final String USER_ID = "userId";

    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final WsSessionRegistry sessionRegistry;
    private final MatchmakingService matchmakingService;
    private final GameService gameService;

    public GameWebSocketHandler(ObjectMapper objectMapper,
                                UserRepository userRepository,
                                WsSessionRegistry sessionRegistry,
                                MatchmakingService matchmakingService,
                                GameService gameService) {
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.sessionRegistry = sessionRegistry;
        this.matchmakingService = matchmakingService;
        this.gameService = gameService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = (Long) session.getAttributes().get(WsTicketInterceptor.ATTR_USER_ID);
        if (userId == null) {
            Authentication authentication = authentication(session);
            if (authentication == null || !authentication.isAuthenticated()) {
                session.close(new CloseStatus(4401, "UNAUTHORIZED"));
                return;
            }
            User user = userRepository.findByUsername(authentication.getName()).orElse(null);
            if (user == null) {
                session.close(new CloseStatus(4401, "UNAUTHORIZED"));
                return;
            }
            userId = user.getId();
        } else if (!userRepository.existsById(userId)) {
            session.close(new CloseStatus(4401, "UNAUTHORIZED"));
            return;
        }
        session.getAttributes().put(USER_ID, userId);
        sessionRegistry.register(userId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return;
        }
        try {
            WsMessage wsMessage = objectMapper.readValue(message.getPayload(), WsMessage.class);
            String op = wsMessage.op();
            if (op == null) {
                throw new ApiException(ErrorCode.BAD_REQUEST, "缺少 op");
            }
            switch (op) {
                case "QUEUE_JOIN" -> matchmakingService.join(userId);
                case "QUEUE_LEAVE" -> matchmakingService.leave(userId);
                case "RESUME" -> gameService.joinRoom(matchId(wsMessage), userId);
                case "MOVE" -> gameService.move(
                        matchId(wsMessage),
                        userId,
                        intField(wsMessage, "x"),
                        intField(wsMessage, "y"),
                        intField(wsMessage, "seq"));
                case "RESIGN" -> gameService.resign(matchId(wsMessage), userId);
                case "PING" -> sessionRegistry.send(userId, "PONG", Map.of());
                default -> throw new ApiException(ErrorCode.BAD_REQUEST, "未知操作: " + op);
            }
        } catch (ApiException ex) {
            sessionRegistry.send(userId, "ERROR", Map.of(
                    "code", ex.getCode().name(),
                    "message", String.valueOf(ex.getMessage())));
        } catch (Exception ex) {
            log.error("failed to handle websocket message", ex);
            sessionRegistry.send(userId, "ERROR", Map.of(
                    "code", ErrorCode.INTERNAL_ERROR.name(),
                    "message", "服务器处理失败"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = currentUserId(session);
        if (userId == null) {
            return;
        }
        sessionRegistry.remove(userId, session);
        if (!sessionRegistry.isOnline(userId)) {
            matchmakingService.leave(userId);
            gameService.disconnect(userId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.debug("websocket transport error", exception);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private Authentication authentication(WebSocketSession session) {
        Object context = session.getAttributes()
                .get(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        if (context instanceof SecurityContext securityContext) {
            return securityContext.getAuthentication();
        }
        return null;
    }

    private Long currentUserId(WebSocketSession session) {
        Object value = session.getAttributes().get(USER_ID);
        return value instanceof Long id ? id : null;
    }

    private Long matchId(WsMessage message) {
        JsonNode data = message.data();
        if (data == null || !data.hasNonNull("matchId")) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "缺少 matchId");
        }
        return data.get("matchId").asLong();
    }

    private int intField(WsMessage message, String field) {
        JsonNode data = message.data();
        if (data == null || !data.hasNonNull(field)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "缺少 " + field);
        }
        return data.get(field).asInt();
    }
}
