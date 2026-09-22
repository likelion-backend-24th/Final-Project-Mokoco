package com.team2.chatservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team2.chatservice.client.UserClient;
import com.team2.common.security.LoginUser;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * chat-service의 사용자 대면 엔드포인트 전용 인증 필터.
 * Authorization: Bearer 토큰을 user-service에 검증해 LoginUser(id, email)를 SecurityContext에 심는다.
 * post-service의 같은 이름 클래스와 동일 패턴.
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
        LoginUser loginUser;
        try {
            var user = users.verifyToken(auth.substring(7));
            if (user == null || user.id() == null || user.email() == null) { error(response, 502, "AUTH_FAILED", "로그인 정보를 확인하지 못했습니다."); return; }
            loginUser = new LoginUser(user.id(), user.email());
        } catch (feign.FeignException e) {
            error(response, e.status() == 401 ? 401 : 502, "AUTH_FAILED", "로그인 정보를 확인하지 못했습니다."); return;
        }
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(loginUser, null, List.of()));
        SecurityContextHolder.setContext(context);
        try { chain.doFilter(request, response); }
        finally { SecurityContextHolder.clearContext(); }
    }
    private void error(HttpServletResponse response, int status, String code, String message) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(status); response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        mapper.writeValue(response.getWriter(), Map.of("code", code, "message", message));
    }
}
