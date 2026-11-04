package com.example.gomoku.user;

import com.example.gomoku.common.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void registerHashesPasswordAndInitializesRating() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterForm form = new RegisterForm();
        form.setUsername("alice");
        form.setNickname("Alice");
        form.setPassword("gomoku123");

        User saved = userService.register(form);

        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getNickname()).isEqualTo("Alice");
        assertThat(saved.getRating()).isEqualTo(1200);
        assertThat(saved.getPasswordHash()).isNotEqualTo("gomoku123");
        assertThat(passwordEncoder.matches("gomoku123", saved.getPasswordHash())).isTrue();
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        RegisterForm form = new RegisterForm();
        form.setUsername("alice");
        form.setNickname("Alice");
        form.setPassword("gomoku123");

        assertThatThrownBy(() -> userService.register(form))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("已被占用");

        verify(userRepository, never()).save(any(User.class));
    }
}
