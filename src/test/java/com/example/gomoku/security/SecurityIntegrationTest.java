package com.example.gomoku.security;

import com.example.gomoku.user.User;
import com.example.gomoku.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityIntegrationTest {

    private static final String USERNAME = "security_it_user";
    private static final String PASSWORD = "gomoku123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        loginAttemptService.clearAll();
        rateLimiter.clearAll();
        if (userRepository.findByUsername(USERNAME).isEmpty()) {
            User user = new User();
            user.setUsername(USERNAME);
            user.setNickname("Security IT");
            user.setPasswordHash(passwordEncoder.encode(PASSWORD));
            user.setRating(1200);
            user.setCreatedAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }

    @Test
    void fiveFailedLoginsLockTheAccount() throws Exception {
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/login")
                            .with(csrf())
                            .param("username", USERNAME)
                            .param("password", "wrong-password"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?error"));
        }

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", USERNAME)
                        .param("password", "wrong-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?locked"));

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", USERNAME)
                        .param("password", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?locked"));
    }

    @Test
    void successfulLoginResetsFailureCounter() throws Exception {
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/login")
                    .with(csrf())
                    .param("username", USERNAME)
                    .param("password", "wrong-password"));
        }

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", USERNAME)
                        .param("password", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lobby"));

        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/login")
                    .with(csrf())
                    .param("username", USERNAME)
                    .param("password", "wrong-password"))
                    .andExpect(redirectedUrl("/login?error"));
        }
    }

    @Test
    void securityHeadersArePresent() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Permissions-Policy", containsString("camera=()")));
    }

    @Test
    void apiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerIsRateLimited() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/register")
                            .with(csrf())
                            .param("username", "rl_user_" + i)
                            .param("nickname", "RL " + i)
                            .param("password", "gomoku123"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));
        }

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "rl_user_x")
                        .param("nickname", "RL X")
                        .param("password", "gomoku123"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("操作过于频繁")));
    }
}
