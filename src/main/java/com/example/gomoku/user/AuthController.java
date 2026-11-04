package com.example.gomoku.user;

import com.example.gomoku.common.ApiException;
import com.example.gomoku.config.SecurityProperties;
import com.example.gomoku.security.AuditLogger;
import com.example.gomoku.security.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;
    private final RateLimiter rateLimiter;
    private final SecurityProperties securityProperties;
    private final AuditLogger auditLogger;

    public AuthController(UserService userService,
                          RateLimiter rateLimiter,
                          SecurityProperties securityProperties,
                          AuditLogger auditLogger) {
        this.userService = userService;
        this.rateLimiter = rateLimiter;
        this.securityProperties = securityProperties;
        this.auditLogger = auditLogger;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegisterForm());
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes,
                           HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        boolean allowed = rateLimiter.tryAcquire(
                "register:" + ip,
                securityProperties.registerMaxPerWindow(),
                securityProperties.registerWindow());
        if (!allowed) {
            bindingResult.reject("register", "操作过于频繁，请稍后再试");
            return "register";
        }

        if (bindingResult.hasErrors()) {
            return "register";
        }
        try {
            userService.register(form);
            auditLogger.registered(form.getUsername(), ip);
        } catch (ApiException e) {
            bindingResult.reject("register", e.getMessage());
            return "register";
        } catch (DataIntegrityViolationException e) {
            bindingResult.reject("register", "用户名已被占用");
            return "register";
        }
        redirectAttributes.addFlashAttribute("registered", true);
        return "redirect:/login";
    }
}
