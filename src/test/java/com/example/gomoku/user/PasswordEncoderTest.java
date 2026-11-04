package com.example.gomoku.user;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @Test
    void bcryptHashIsSaltedAndVerifiable() {
        String hash = encoder.encode("gomoku123");

        assertThat(hash).startsWith("$2");
        assertThat(encoder.matches("gomoku123", hash)).isTrue();
        assertThat(encoder.matches("gomoku124", hash)).isFalse();
        assertThat(encoder.encode("gomoku123")).isNotEqualTo(hash);
    }
}
