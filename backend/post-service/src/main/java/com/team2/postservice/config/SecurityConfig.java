package com.team2.postservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team2.postservice.client.UserClient;
import com.team2.postservice.common.security.TokenAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain authenticatedApiSecurityFilterChain(
            HttpSecurity http,
            UserClient users,
            ObjectMapper mapper) throws Exception {
        return http
                .securityMatcher("/posts/**", "/fix-deals/**", "/notifications/**", "/reviews/**",
                        "/profile/**", "/resumes/**", "/api/ai/**", "/api/chat-rooms/**")
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .requestCache(cache -> cache.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new TokenAuthenticationFilter(users, mapper, true),
                        AnonymousAuthenticationFilter.class)
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, failure) -> {
                    response.setStatus(401); response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
                    mapper.writeValue(response.getWriter(), java.util.Map.of("code", "LOGIN_REQUIRED", "message", "로그인이 필요합니다."));
                }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/resumes/me").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/posts", "/posts/{id}",
                                "/posts/{id}/proposals", "/reviews/**", "/resumes/{userId}").permitAll()
                        .anyRequest().authenticated())
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // 우선 모든 요청 허용 (추후 JWT 필터 추가 가능)
                );

        return http.build();
    }
}
