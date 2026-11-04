package com.example.gomoku.chat;

import com.example.gomoku.common.ApiException;
import com.example.gomoku.security.RateLimiter;
import com.example.gomoku.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;

    private MutableClock clock;
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        clock = MutableClock.at("2026-01-01T00:00:00Z");
        chatService = new ChatService(chatRepository, new RateLimiter(clock));
    }

    @Test
    void trimsAndSavesMessage() {
        when(chatRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatMessage message = chatService.send(1L, 7L, "  hello  ");

        assertThat(message.getContent()).isEqualTo("hello");
        assertThat(message.getMatchId()).isEqualTo(1L);
        assertThat(message.getSenderId()).isEqualTo(7L);
    }

    @Test
    void rejectsBlankMessage() {
        assertThatThrownBy(() -> chatService.send(1L, 7L, "   "))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("不能为空");
    }

    @Test
    void rejectsTooLongMessage() {
        assertThatThrownBy(() -> chatService.send(1L, 7L, "x".repeat(ChatService.MAX_LENGTH + 1)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("最多");
    }

    @Test
    void rateLimitsToOneMessagePerSecond() {
        when(chatRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        chatService.send(1L, 7L, "first");

        assertThatThrownBy(() -> chatService.send(1L, 7L, "second"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("频繁");

        clock.advance(Duration.ofMillis(1100));

        assertThat(chatService.send(1L, 7L, "third").getContent()).isEqualTo("third");
    }
}
