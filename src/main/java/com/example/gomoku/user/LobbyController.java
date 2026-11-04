package com.example.gomoku.user;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LobbyController {

    private final UserService userService;

    public LobbyController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/lobby";
    }

    @GetMapping("/lobby")
    public String lobby(Authentication authentication, Model model) {
        User user = userService.requireByUsername(authentication.getName());
        model.addAttribute("user", user);
        return "lobby";
    }

    @GetMapping("/leaderboard")
    public String leaderboard() {
        return "leaderboard";
    }
}
