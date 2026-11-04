package com.example.gomoku.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 在认证之前拦截已锁定的账号，避免继续尝试密码。
 */
public class LoginLockFilter extends OncePerRequestFilter {

    private final LoginAttemptService loginAttemptService;

    public LoginLockFilter(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if ("POST".equalsIgnoreCase(request.getMethod()) && "/login".equals(path)) {
            String username = request.getParameter("username");
            if (loginAttemptService.isBlocked(username, request.getRemoteAddr())) {
                response.sendRedirect(request.getContextPath() + "/login?locked");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
