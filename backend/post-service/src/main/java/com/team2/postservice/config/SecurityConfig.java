package com.team2.postservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // AI 계약 초안 작성 관련 엔드포인트만 Authorization: Bearer로 직접 인증한다. 채팅방 개설/조회
    // (proposals/*, */detail)는 chat-service로 옮겨갔다 — 나머지 post-service 엔드포인트는
    // 기존처럼 X-User-Email(BFF가 검증 후 주입) 방식을 그대로 쓴다.
    @Bean
    @org.springframework.core.annotation.Order(1)
    public SecurityFilterChain authenticatedApiSecurityFilterChain(HttpSecurity http,
            com.team2.postservice.client.UserClient users, com.fasterxml.jackson.databind.ObjectMapper mapper) throws Exception {
        return http
                .securityMatcher("/api/ai/**", "/api/chat-rooms/*/contract/ai-draft")
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .requestCache(cache -> cache.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new com.team2.postservice.common.security.TokenAuthenticationFilter(users, mapper),
                        org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .build();
    }

    @Bean
    @org.springframework.core.annotation.Order(2)
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