package com.team2.chatservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

// user-service/post-service와 동일한 패턴(X-Internal-Service-Key 공유 시크릿) —
// post-service가 chat-service의 /api/internal/** 를 호출할 때 쓴다.
public class InternalServiceAuthenticationFilter extends OncePerRequestFilter {
    private final byte[] expectedKey;

    public InternalServiceAuthenticationFilter(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("INTERNAL_SERVICE_KEY must not be blank");
        expectedKey = key.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String suppliedKey = request.getHeader("X-Internal-Service-Key");
        if (suppliedKey == null || !MessageDigest.isEqual(expectedKey, suppliedKey.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"code\":\"UNAUTHORIZED_INTERNAL_SERVICE\",\"message\":\"Internal service authentication required\"}");
            return;
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                "post-service", null, List.of(new SimpleGrantedAuthority("INTERNAL_SERVICE"))));
        SecurityContextHolder.setContext(context);
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
