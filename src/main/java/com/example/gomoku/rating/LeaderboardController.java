package com.example.gomoku.rating;

import com.example.gomoku.user.User;
import com.example.gomoku.user.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class LeaderboardController {

    private final UserService userService;
    private final LeaderboardService leaderboardService;

    public LeaderboardController(UserService userService, LeaderboardService leaderboardService) {
        this.userService = userService;
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/leaderboard")
    public String page(Authentication authentication, Model model) {
        User me = userService.requireByUsername(authentication.getName());
        model.addAttribute("me", me);
        model.addAttribute("entries", leaderboardService.top(50));
        model.addAttribute("myRank", leaderboardService.rankOf(me));
        return "leaderboard";
    }

    @GetMapping("/api/leaderboard")
    @ResponseBody
    public Map<String, Object> api(Authentication authentication) {
        User me = userService.requireByUsername(authentication.getName());
        return Map.of(
                "entries", leaderboardService.top(50),
                "myRank", leaderboardService.rankOf(me));
    }
}
