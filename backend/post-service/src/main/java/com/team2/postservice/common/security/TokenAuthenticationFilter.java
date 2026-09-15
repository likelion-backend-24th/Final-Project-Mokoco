package com.team2.postservice.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team2.postservice.ai.AiRequestTrace;
import com.team2.postservice.client.UserClient;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 채택 전 제안 채팅(/api/chat-rooms/proposals/**, /api/chat-rooms/*&#47;detail) 전용 인증 필터.
 * 다른 post-service 엔드포인트(X-User-Email 기반)와는 별개로, Authorization: Bearer 토큰을
 * user-service에 검증해 LoginUser(id)를 SecurityContext에 심는다.
 * Registered only in the token-authenticated security chain, not as a servlet filter bean.
 */
public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private final UserClient users;
    private final ObjectMapper mapper;
    public TokenAuthenticationFilter(UserClient users, ObjectMapper mapper) { this.users = users; this.mapper = mapper; }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ") || auth.substring(7).isBlank()) {
            error(response, 401, "LOGIN_REQUIRED", "로그인이 필요합니다."); return;
        }
        Long userId;
        try {
            var user = users.verifyToken(auth.substring(7));
            if (user == null || user.id() == null) { error(response, 502, "AUTH_FAILED", "로그인 정보를 확인하지 못했습니다."); return; }
            userId = user.id();
        } catch (feign.FeignException e) {
            error(response, e.status() == 401 ? 401 : 502, "AUTH_FAILED", "로그인 정보를 확인하지 못했습니다."); return;
        }
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(new LoginUser(userId), null, List.of()));
        SecurityContextHolder.setContext(context);
        try { chain.doFilter(request, response); }
        finally { SecurityContextHolder.clearContext(); }
    }
    private void error(HttpServletResponse response, int status, String code, String message) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(status); response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        mapper.writeValue(response.getWriter(), Map.of("code", code, "message", message, "requestId", AiRequestTrace.requestId()));
    }
}
