package com.example.gomoku.game;

import com.example.gomoku.common.ApiException;
import com.example.gomoku.common.ErrorCode;
import com.example.gomoku.user.User;
import com.example.gomoku.user.UserService;
import com.example.gomoku.ws.WsTicketService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class GameController {

    private final GameService gameService;
    private final UserService userService;
    private final WsTicketService wsTicketService;

    public GameController(GameService gameService, UserService userService, WsTicketService wsTicketService) {
        this.gameService = gameService;
        this.userService = userService;
        this.wsTicketService = wsTicketService;
    }

    @GetMapping("/game/{id}")
    public String gamePage(@PathVariable Long id, Authentication authentication, Model model) {
        User me = userService.requireByUsername(authentication.getName());
        GameMatch match = gameService.getMatch(id);

        boolean isBlack = match.getBlackId().equals(me.getId());
        boolean isWhite = match.getWhiteId().equals(me.getId());
        if (!isBlack && !isWhite) {
            throw new ApiException(ErrorCode.FORBIDDEN, "无权访问该对局");
        }

        Long opponentId = isBlack ? match.getWhiteId() : match.getBlackId();
        User opponent = userService.requireById(opponentId);

        model.addAttribute("match", match);
        model.addAttribute("me", me);
        model.addAttribute("opponent", opponent);
        model.addAttribute("myColor", isBlack ? "BLACK" : "WHITE");
        model.addAttribute("wsTicket", wsTicketService.issue(me.getId()));
        return "game";
    }
}
