package com.example.gomoku.user;

import com.example.gomoku.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, LobbyController.class})
@Import(SecurityConfig.class)
class PageRenderTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void loginPageRenders() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("五子棋在线对战")));
    }

    @Test
    void registerPageRenders() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("注册账号")));
    }

    @Test
    void lobbyRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/lobby"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "alice")
    void lobbyRendersForAuthenticatedUser() throws Exception {
        User user = new User();
        user.setUsername("alice");
        user.setNickname("Alice");
        user.setRating(1200);
        user.setCreatedAt(LocalDateTime.now());
        when(userService.requireByUsername("alice")).thenReturn(user);

        mockMvc.perform(get("/lobby"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Alice")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("1200")));
    }
}
