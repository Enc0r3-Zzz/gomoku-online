package com.example.gomoku.game;

import com.example.gomoku.common.ApiException;
import com.example.gomoku.user.User;
import com.example.gomoku.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@TestPropertySource(properties = "gomoku.scheduling.enabled=false")
class GameServiceTest {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    @Autowired
    private GameService gameService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MoveRepository moveRepository;

    private User black;
    private User white;

    @BeforeEach
    void setUp() {
        black = createUser("black");
        white = createUser("white");
    }

    @Test
    void blackWinsAfterFiveInARow() {
        GameMatch match = startGame();

        move(match, black, 7, 7, 1);
        move(match, white, 0, 0, 2);
        move(match, black, 8, 7, 3);
        move(match, white, 0, 1, 4);
        move(match, black, 9, 7, 5);
        move(match, white, 0, 2, 6);
        move(match, black, 10, 7, 7);
        move(match, white, 0, 3, 8);
        move(match, black, 11, 7, 9);

        GameMatch finished = gameService.getMatch(match.getId());
        assertThat(finished.getStatus()).isEqualTo(GameMatchStatus.FINISHED);
        assertThat(finished.getResult()).isEqualTo(GameResult.BLACK_WIN);
        assertThat(finished.getWinnerId()).isEqualTo(black.getId());
        assertThat(moveRepository.findByMatchIdOrderBySeqAsc(match.getId())).hasSize(9);
        assertThat(userRepository.findById(black.getId()).orElseThrow().getWins()).isEqualTo(1);
        assertThat(userRepository.findById(white.getId()).orElseThrow().getLosses()).isEqualTo(1);
    }

    @Test
    void rejectsMoveWhenNotYourTurn() {
        GameMatch match = startGame();

        assertThatThrownBy(() -> gameService.move(match.getId(), white.getId(), 7, 7, 1))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("还没轮到");
    }

    @Test
    void rejectsWrongSequenceNumber() {
        GameMatch match = startGame();
        move(match, black, 7, 7, 1);

        assertThatThrownBy(() -> gameService.move(match.getId(), white.getId(), 8, 8, 3))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("序号");
    }

    @Test
    void rejectsOccupiedCell() {
        GameMatch match = startGame();
        move(match, black, 7, 7, 1);

        assertThatThrownBy(() -> gameService.move(match.getId(), white.getId(), 7, 7, 2))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("落点非法");
    }

    @Test
    void resignHandsWinToOpponent() {
        GameMatch match = startGame();

        gameService.resign(match.getId(), black.getId());

        GameMatch finished = gameService.getMatch(match.getId());
        assertThat(finished.getStatus()).isEqualTo(GameMatchStatus.FINISHED);
        assertThat(finished.getResult()).isEqualTo(GameResult.WHITE_WIN);
        assertThat(finished.getWinnerId()).isEqualTo(white.getId());
        assertThat(userRepository.findById(white.getId()).orElseThrow().getWins()).isEqualTo(1);
    }

    @Test
    void onlyParticipantsCanJoin() {
        GameMatch match = startGame();
        User outsider = createUser("outsider");

        assertThatThrownBy(() -> gameService.joinRoom(match.getId(), outsider.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("不是本局玩家");
    }

    private GameMatch startGame() {
        GameMatch match = gameService.createMatch(black.getId(), white.getId());
        gameService.joinRoom(match.getId(), black.getId());
        gameService.joinRoom(match.getId(), white.getId());
        assertThat(gameService.getMatch(match.getId()).getStatus()).isEqualTo(GameMatchStatus.PLAYING);
        return match;
    }

    private void move(GameMatch match, User player, int x, int y, int seq) {
        gameService.move(match.getId(), player.getId(), x, y, seq);
    }

    private User createUser(String prefix) {
        String suffix = Integer.toString(COUNTER.incrementAndGet(), 36) + System.nanoTime() % 100000;
        User user = new User();
        user.setUsername(prefix + "_" + suffix);
        user.setNickname(prefix);
        user.setPasswordHash("$2a$12$not-used-in-this-test");
        user.setRating(1200);
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
}
