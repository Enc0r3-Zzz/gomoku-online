package com.example.gomoku.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final LoginAttemptService loginAttemptService;
    private final AuditLogger auditLogger;

    public LoginFailureHandler(LoginAttemptService loginAttemptService, AuditLogger auditLogger) {
        this.loginAttemptService = loginAttemptService;
        this.auditLogger = auditLogger;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String username = request.getParameter("username");
        String ip = request.getRemoteAddr();
        loginAttemptService.loginFailed(username, ip);
        auditLogger.loginFailed(username, ip);

        boolean blocked = loginAttemptService.isBlocked(username, ip);
        if (blocked) {
            auditLogger.accountLocked(username, ip);
        }
        String target = blocked ? "/login?locked" : "/login?error";
        response.sendRedirect(request.getContextPath() + target);
    }
}
