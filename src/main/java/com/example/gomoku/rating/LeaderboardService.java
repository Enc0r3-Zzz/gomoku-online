package com.example.gomoku.rating;

import com.example.gomoku.user.User;
import com.example.gomoku.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class LeaderboardService {

    private final UserRepository userRepository;

    public LeaderboardService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntry> top(int limit) {
        int size = Math.max(1, Math.min(limit, 100));
        Pageable pageable = PageRequest.of(0, size,
                Sort.by(Sort.Order.desc("rating"), Sort.Order.asc("id")));
        List<User> users = userRepository.findAll(pageable).getContent();

        List<LeaderboardEntry> entries = new ArrayList<>();
        int rank = 0;
        Integer previousRating = null;
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            if (previousRating == null || previousRating != user.getRating()) {
                rank = i + 1;
                previousRating = user.getRating();
            }
            entries.add(new LeaderboardEntry(
                    rank,
                    user.getId(),
                    user.getNickname(),
                    user.getRating(),
                    user.getWins(),
                    user.getLosses(),
                    user.getDraws()));
        }
        return entries;
    }

    @Transactional(readOnly = true)
    public long rankOf(User user) {
        return userRepository.countByRatingGreaterThan(user.getRating()) + 1;
    }

    public record LeaderboardEntry(int rank,
                                   Long id,
                                   String nickname,
                                   int rating,
                                   int wins,
                                   int losses,
                                   int draws) {
    }
}
