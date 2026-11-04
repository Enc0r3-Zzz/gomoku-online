package com.example.gomoku.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final LoginAttemptService loginAttemptService;
    private final AuditLogger auditLogger;

    public LoginSuccessHandler(LoginAttemptService loginAttemptService, AuditLogger auditLogger) {
        this.loginAttemptService = loginAttemptService;
        this.auditLogger = auditLogger;
        setDefaultTargetUrl("/lobby");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String username = authentication.getName();
        String ip = request.getRemoteAddr();
        loginAttemptService.loginSucceeded(username, ip);
        auditLogger.loginSucceeded(username, ip);
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
