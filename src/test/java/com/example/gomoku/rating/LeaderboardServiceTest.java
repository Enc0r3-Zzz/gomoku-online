package com.example.gomoku.rating;

import com.example.gomoku.user.User;
import com.example.gomoku.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@TestPropertySource(properties = "gomoku.scheduling.enabled=false")
class LeaderboardServiceTest {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    @Autowired
    private LeaderboardService leaderboardService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void ordersByRatingAndSharesRankOnTies() {
        User top = createUser(2000);
        User second = createUser(1800);
        User third = createUser(1300);
        User fourth = createUser(1300);

        List<LeaderboardService.LeaderboardEntry> entries = leaderboardService.top(10);

        assertThat(entries.get(0).nickname()).isEqualTo(top.getNickname());
        assertThat(entries.get(0).rank()).isEqualTo(1);
        assertThat(entries.get(1).nickname()).isEqualTo(second.getNickname());
        assertThat(entries.get(1).rank()).isEqualTo(2);
        assertThat(entries.get(2).rank()).isEqualTo(3);
        assertThat(entries.get(3).rank()).isEqualTo(3);
        assertThat(entries.get(2).id()).isIn(third.getId(), fourth.getId());
        assertThat(entries.get(3).id()).isIn(third.getId(), fourth.getId());

        assertThat(leaderboardService.rankOf(top)).isEqualTo(1);
        assertThat(leaderboardService.rankOf(third)).isEqualTo(3);
    }

    private User createUser(int rating) {
        String suffix = Integer.toString(COUNTER.incrementAndGet(), 36) + System.nanoTime() % 100000;
        User user = new User();
        user.setUsername("lb_" + suffix);
        user.setNickname("LB" + suffix);
        user.setPasswordHash("$2a$12$not-used-in-this-test");
        user.setRating(rating);
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
}
