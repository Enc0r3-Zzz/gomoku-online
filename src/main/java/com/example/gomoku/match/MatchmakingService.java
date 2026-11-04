package com.example.gomoku.match;

import com.example.gomoku.common.ApiException;
import com.example.gomoku.common.ErrorCode;
import com.example.gomoku.game.GameMatch;
import com.example.gomoku.game.GameService;
import com.example.gomoku.user.User;
import com.example.gomoku.user.UserRepository;
import com.example.gomoku.ws.WsSessionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class MatchmakingService {

    private static final Logger log = LoggerFactory.getLogger(MatchmakingService.class);

    private static final int BASE_RATING_BAND = 100;
    private static final int MAX_RATING_BAND = 400;
    private static final int BAND_GROWTH_PER_SECONDS = 5;
    private static final int BAND_GROWTH_STEP = 50;

    private final List<WaitingPlayer> queue = new ArrayList<>();
    private final GameService gameService;
    private final UserRepository userRepository;
    private final WsSessionRegistry sessionRegistry;

    public MatchmakingService(GameService gameService,
                              UserRepository userRepository,
                              WsSessionRegistry sessionRegistry) {
        this.gameService = gameService;
        this.userRepository = userRepository;
        this.sessionRegistry = sessionRegistry;
    }

    public synchronized void join(Long userId) {
        if (queue.stream().anyMatch(player -> player.userId().equals(userId))) {
            sessionRegistry.send(userId, "ERROR", Map.of(
                    "code", ErrorCode.ALREADY_QUEUED.name(),
                    "message", "已经在匹配队列中"));
            return;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "用户不存在"));
        queue.add(new WaitingPlayer(userId, user.getRating(), user.getNickname(), Instant.now()));
        sessionRegistry.send(userId, "QUEUED", Map.of("since", Instant.now().toEpochMilli()));
    }

    public synchronized void leave(Long userId) {
        queue.removeIf(player -> player.userId().equals(userId));
    }

    public void pair() {
        WaitingPlayer first;
        WaitingPlayer second;
        synchronized (this) {
            queue.removeIf(player -> !sessionRegistry.isOnline(player.userId()));
            if (queue.size() < 2) {
                return;
            }
            queue.sort(Comparator.comparing(WaitingPlayer::since));

            int i = -1;
            int j = -1;
            for (int a = 0; a < queue.size() && i < 0; a++) {
                for (int b = a + 1; b < queue.size(); b++) {
                    if (ratingDistanceAllowed(queue.get(a), queue.get(b))) {
                        i = a;
                        j = b;
                        break;
                    }
                }
            }
            if (i < 0) {
                return;
            }
            first = queue.remove(j);
            second = queue.remove(i);
        }

        GameMatch match;
        try {
            boolean firstIsBlack = Math.random() < 0.5;
            match = firstIsBlack
                    ? gameService.createMatch(first.userId(), second.userId())
                    : gameService.createMatch(second.userId(), first.userId());
        } catch (RuntimeException ex) {
            log.error("failed to create match for {} and {}", first.userId(), second.userId(), ex);
            return;
        }

        Long blackId = match.getBlackId();
        notifyMatchFound(first, second, match, blackId);
        notifyMatchFound(second, first, match, blackId);
    }

    private void notifyMatchFound(WaitingPlayer player, WaitingPlayer opponent,
                                  GameMatch match, Long blackId) {
        boolean isBlack = player.userId().equals(blackId);
        sessionRegistry.send(player.userId(), "MATCH_FOUND", Map.of(
                "matchId", match.getId(),
                "color", isBlack ? "BLACK" : "WHITE",
                "opponent", Map.of(
                        "id", opponent.userId(),
                        "nickname", opponent.nickname(),
                        "rating", opponent.rating())));
    }

    private boolean ratingDistanceAllowed(WaitingPlayer a, WaitingPlayer b) {
        int allowed = allowedBand(a);
        return Math.abs(a.rating() - b.rating()) <= allowed;
    }

    private int allowedBand(WaitingPlayer player) {
        long waitedSeconds = Math.max(0, Instant.now().getEpochSecond() - player.since().getEpochSecond());
        int allowed = BASE_RATING_BAND + (int) (waitedSeconds / BAND_GROWTH_PER_SECONDS) * BAND_GROWTH_STEP;
        return Math.min(MAX_RATING_BAND, allowed);
    }

    private record WaitingPlayer(Long userId, int rating, String nickname, Instant since) {
    }
}
