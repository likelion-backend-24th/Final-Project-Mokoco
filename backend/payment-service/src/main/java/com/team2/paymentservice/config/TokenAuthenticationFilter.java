package com.team2.paymentservice.config;

import com.team2.common.security.LoginUser;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.client.*;
import java.io.IOException;
import java.util.List;

public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private final RestClient users;
    public TokenAuthenticationFilter(RestClient users) { this.users = users; }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VerifiedUser(Long id, String email) {}

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ") || auth.substring(7).isBlank()) {
            response.sendError(401); return;
        }
        VerifiedUser user;
        try {
            user = users.post().uri("/api/internal/users/verify-token").body(auth.substring(7))
                    .retrieve().body(VerifiedUser.class);
        } catch (RestClientResponseException failure) {
            boolean internal = failure.getResponseHeaders() != null
                    && failure.getResponseHeaders().containsKey("X-Internal-Auth-Error");
            response.sendError(failure.getStatusCode().value() == 401 && !internal ? 401 : 502); return;
        } catch (RestClientException failure) {
            response.sendError(502); return;
        }
        if (user == null || user.id() == null || user.id() <= 0 || user.email() == null || user.email().isBlank()) {
            response.sendError(502); return;
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(new LoginUser(user.id(), user.email()), null, List.of()));
        SecurityContextHolder.setContext(context);
        try { chain.doFilter(request, response); }
        finally { SecurityContextHolder.clearContext(); }
    }
}
