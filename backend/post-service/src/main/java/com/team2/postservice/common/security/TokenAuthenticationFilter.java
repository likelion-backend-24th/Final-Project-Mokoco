package com.team2.postservice.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team2.common.security.LoginUser;
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
 * post-service의 모든 사용자 대면 엔드포인트(공개 조회 제외) 공통 인증 필터.
 * Authorization: Bearer 토큰을 user-service에 검증해 LoginUser(id, email)를 SecurityContext에 심는다.
 * Registered only in the token-authenticated security chain, not as a servlet filter bean.
 */
public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private final UserClient users;
    private final ObjectMapper mapper;
    private final boolean allowAnonymous;
    public TokenAuthenticationFilter(UserClient users, ObjectMapper mapper) { this(users, mapper, false); }
    // allowAnonymous=true: 토큰이 없거나 검증에 실패해도 막지 않고 비로그인으로 통과시킨다
    // (예: 글 목록 GET — 로그인했으면 개인화하지만 비로그인도 봐야 하는 엔드포인트용).
    public TokenAuthenticationFilter(UserClient users, ObjectMapper mapper, boolean allowAnonymous) {
        this.users = users; this.mapper = mapper; this.allowAnonymous = allowAnonymous;
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ") || auth.substring(7).isBlank()) {
            if (allowAnonymous) { chain.doFilter(request, response); return; }
            error(response, 401, "LOGIN_REQUIRED", "로그인이 필요합니다."); return;
        }
        LoginUser loginUser;
        try {
            var user = users.verifyToken(auth.substring(7));
            if (user == null || user.id() == null || user.email() == null) {
                if (allowAnonymous) { chain.doFilter(request, response); return; }
                error(response, 502, "AUTH_FAILED", "로그인 정보를 확인하지 못했습니다."); return;
            }
            loginUser = new LoginUser(user.id(), user.email());
        } catch (feign.FeignException e) {
            if (allowAnonymous) { chain.doFilter(request, response); return; }
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
        mapper.writeValue(response.getWriter(), Map.of("code", code, "message", message, "requestId", AiRequestTrace.requestId()));
    }
}
