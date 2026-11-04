package com.example.gomoku.game;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameMatchRepository extends JpaRepository<GameMatch, Long> {

    List<GameMatch> findByStatus(GameMatchStatus status);
}
