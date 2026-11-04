package com.example.gomoku.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final GameWebSocketHandler gameWebSocketHandler;
    private final WsTicketService wsTicketService;

    public WebSocketConfig(GameWebSocketHandler gameWebSocketHandler, WsTicketService wsTicketService) {
        this.gameWebSocketHandler = gameWebSocketHandler;
        this.wsTicketService = wsTicketService;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gameWebSocketHandler, "/ws/game")
                .addInterceptors(new HttpSessionHandshakeInterceptor(),
                        new WsTicketInterceptor(wsTicketService))
                .setAllowedOriginPatterns("*");
    }
}
