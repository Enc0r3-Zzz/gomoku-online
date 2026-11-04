package com.example.gomoku.rating;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RatingHistoryRepository extends JpaRepository<RatingHistory, Long> {

    List<RatingHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<RatingHistory> findByMatchId(Long matchId);

    long countByUserId(Long userId);
}
