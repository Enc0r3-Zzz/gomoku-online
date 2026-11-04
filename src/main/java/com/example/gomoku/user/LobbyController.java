package com.example.gomoku.user;

import com.example.gomoku.ws.WsTicketService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class LobbyController {

    private final UserService userService;
    private final WsTicketService wsTicketService;

    public LobbyController(UserService userService, WsTicketService wsTicketService) {
        this.userService = userService;
        this.wsTicketService = wsTicketService;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/lobby";
    }

    @GetMapping("/lobby")
    public String lobby(Authentication authentication, Model model) {
        User user = userService.requireByUsername(authentication.getName());
        model.addAttribute("user", user);
        model.addAttribute("wsTicket", wsTicketService.issue(user.getId()));
        return "lobby";
    }

    @GetMapping("/leaderboard")
    public String leaderboard() {
        return "leaderboard";
    }

    @GetMapping("/api/me")
    @ResponseBody
    public Map<String, Object> me(Authentication authentication) {
        User user = userService.requireByUsername(authentication.getName());
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "nickname", user.getNickname(),
                "rating", user.getRating(),
                "wins", user.getWins(),
                "losses", user.getLosses(),
                "draws", user.getDraws());
    }
}
