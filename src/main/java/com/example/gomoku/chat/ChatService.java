package com.example.gomoku.chat;

import com.example.gomoku.common.ApiException;
import com.example.gomoku.common.ErrorCode;
import com.example.gomoku.security.RateLimiter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ChatService {

    public static final int MAX_LENGTH = 200;
    private static final Duration MIN_INTERVAL = Duration.ofSeconds(1);

    private final ChatRepository chatRepository;
    private final RateLimiter rateLimiter;

    public ChatService(ChatRepository chatRepository, RateLimiter rateLimiter) {
        this.chatRepository = chatRepository;
        this.rateLimiter = rateLimiter;
    }

    @Transactional
    public ChatMessage send(Long matchId, Long senderId, String text) {
        String content = text == null ? "" : text.strip();
        if (content.isEmpty()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "消息不能为空");
        }
        if (content.length() > MAX_LENGTH) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "消息最多 " + MAX_LENGTH + " 字");
        }
        if (!rateLimiter.tryAcquire("chat:" + senderId, 1, MIN_INTERVAL)) {
            throw new ApiException(ErrorCode.RATE_LIMITED, "发送过于频繁，请稍后再试");
        }

        ChatMessage message = new ChatMessage();
        message.setMatchId(matchId);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        return chatRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> recent(Long matchId) {
        List<ChatMessage> messages = new ArrayList<>(
                chatRepository.findTop50ByMatchIdOrderByIdDesc(matchId));
        Collections.reverse(messages);
        return messages;
    }
}
