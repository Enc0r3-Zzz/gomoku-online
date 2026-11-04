package com.example.gomoku.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findTop50ByMatchIdOrderByIdDesc(Long matchId);

    long countByMatchId(Long matchId);
}
