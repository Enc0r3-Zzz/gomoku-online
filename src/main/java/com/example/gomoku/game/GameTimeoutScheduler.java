package com.example.gomoku.game;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "gomoku.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class GameTimeoutScheduler {

    private final GameService gameService;

    public GameTimeoutScheduler(GameService gameService) {
        this.gameService = gameService;
    }

    @Scheduled(fixedDelay = 1000)
    public void checkTimeouts() {
        gameService.timeoutExpired();
    }
}
