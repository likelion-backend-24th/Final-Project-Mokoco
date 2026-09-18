package com.team2.postservice.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team2.postservice.ai.AiRequestTrace;
import com.team2.postservice.client.UserClient;
import com.team2.postservice.client.dto.UserClientResponse;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Registered only in the token-authenticated security chain, not as a servlet filter bean. */
public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private final UserClient users;
    private final ObjectMapper mapper;
    public TokenAuthenticationFilter(UserClient users, ObjectMapper mapper) { this.users = users; this.mapper = mapper; }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException
    {
        String auth = request.getHeader("Authorization");

        if (auth == null || !auth.startsWith("Bearer ") || auth.substring(7).isBlank()) {
            error(response,401,"LOGIN_REQUIRED","로그인이 필요합니다."); return;
        }

        Long userId;

        try {
            UserClientResponse user = users.verifyToken(auth.substring(7));

            if (user == null || user.id() == null) {
                error(response,502,"AUTH_FAILED","로그인 정보를 확인하지 못했습니다."); return;
            }

            userId = user.id();

        } catch (feign.FeignException e) {
            error(response,e.status() == 401 ? 401 : 502,"AUTH_FAILED","로그인 정보를 확인하지 못했습니다."); return;
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();

        context.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        new LoginUser(userId),
                        null,List.of())
        );

        SecurityContextHolder.setContext(context);

        try {
            chain.doFilter(request,response);
        }
        finally {
            SecurityContextHolder.clearContext();
        }
    }
    private void error(HttpServletResponse response, int status, String code, String message) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(status); response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control","no-store");
        mapper.writeValue(
                response.getWriter(),
                Map.of("code",code,"message",message,"requestId",AiRequestTrace.requestId())
        );
    }
}
