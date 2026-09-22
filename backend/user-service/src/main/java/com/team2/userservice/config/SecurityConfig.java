package com.team2.userservice.config;

import com.team2.userservice.user.repository.UserRepository;
import com.team2.userservice.user.service.CustomOAuth2UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler; // 주입 추가

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults()) // CORS 활성화 (필요시 CorsConfigurationSource 빈 등록)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/error", "/oauth2/**", "/login/**").permitAll()
                        .requestMatchers("/api/regions/test").permitAll()
                        .requestMatchers("/api/internal/**").permitAll()
                        .requestMatchers("/policy/**").permitAll() // 정책 페이지(개인정보처리방침 등) 공개 접근
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll() // API 문서
                        .requestMatchers("/api/users/me/region").authenticated() // 명시적 지정
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"); // 302 대신 401 반환
                        })
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, userRepository), UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(c -> c.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        // 계정 연결 흐름에서 다른 계정에 이미 연결된 소셜 계정이면 CustomOAuth2UserService가
                        // OAuth2AuthenticationException을 던진다 — 기본 /login?error 대신 프론트 리다이렉트
                        // 페이지로 보내 에러 코드를 보여준다.
                        .failureHandler((request, response, exception) -> {
                            String code = exception instanceof OAuth2AuthenticationException oauthEx
                                    && oauthEx.getError().getErrorCode() != null
                                    ? oauthEx.getError().getErrorCode() : "oauth_failed";
                            response.sendRedirect("https://32.199.114.190.nip.io/oauth2/redirect?error=" + code);
                        })
                );

        return http.build();
    }
}