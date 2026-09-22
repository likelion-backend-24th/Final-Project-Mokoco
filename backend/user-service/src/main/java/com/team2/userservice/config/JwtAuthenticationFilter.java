package com.team2.userservice.config;

import com.team2.common.security.LoginUser;
import com.team2.userservice.user.entity.AccountStatus;
import com.team2.userservice.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && jwtTokenProvider.validateAccessToken(token)) {
            Long userId = jwtTokenProvider.getUserIdFromAccessToken(token);
            var user = userRepository.findById(userId).orElse(null);

            // 토큰 자체는 아직 유효해도, 그 사이 관리자가 계정을 정지시켰을 수 있으므로
            // 요청마다 DB에서 현재 상태를 다시 확인한다(정지되면 인증을 아예 심지 않아 401로 막힘).
            if (user != null && user.getStatus() != AccountStatus.SUSPENDED) {
                // post/chat/payment-service와 같은 LoginUser(id, email) principal을 심는다.
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(new LoginUser(user.getId(), user.getEmail()), null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER")));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        // 1. 헤더에서 토큰 추출 시도
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 2. 헤더에 없다면 쿠키에서 'access_token' 추출 시도
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) { // 실제 쿠키 이름에 맞춰 변경 가능 (예: token 등)
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}