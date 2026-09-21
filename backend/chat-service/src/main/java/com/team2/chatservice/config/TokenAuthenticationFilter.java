package com.team2.chatservice.config;

import com.team2.chatservice.client.UserClient;
import com.team2.chatservice.client.dto.UserClientResponse;
import com.team2.common.security.LoginUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private final UserClient users;
    public TokenAuthenticationFilter(UserClient users) { this.users = users; }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                             FilterChain chain) throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ") || auth.substring(7).isBlank()) {
            response.sendError(401);
            return;
        }
        UserClientResponse user;
        try {
            user = users.verifyToken(auth.substring(7));
        } catch (org.springframework.web.server.ResponseStatusException failure) {
            response.sendError(failure.getStatusCode().value());
            return;
        } catch (feign.FeignException failure) {
            response.sendError(failure.status() == 401 ? 401 : 502);
            return;
        }
        if (user == null || user.id() == null) {
            response.sendError(502);
            return;
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(new LoginUser(user.id(), user.email()), null, List.of()));
        SecurityContextHolder.setContext(context);
        try { chain.doFilter(request, response); }
        finally { SecurityContextHolder.clearContext(); }
    }
}
