package com.example.gomoku.match;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "gomoku.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class MatchmakingScheduler {

    private final MatchmakingService matchmakingService;

    public MatchmakingScheduler(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @Scheduled(fixedDelay = 1000)
    public void pair() {
        matchmakingService.pair();
    }
}
