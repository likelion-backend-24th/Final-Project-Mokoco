package com.team2.chatservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 채택 전 제안 채팅(견적별 채팅방 개설/조회, 방 상세)만 Authorization: Bearer로 직접 인증한다.
    // 나머지 chat-service 엔드포인트는 post-service와 같은 방식(X-User-Email 또는 컨트롤러가
    // 직접 Bearer를 파싱)을 그대로 쓴다.
    @Bean
    @Order(1)
    public SecurityFilterChain authenticatedApiSecurityFilterChain(HttpSecurity http,
            com.team2.chatservice.client.UserClient users, com.fasterxml.jackson.databind.ObjectMapper mapper) throws Exception {
        return http
                .securityMatcher("/api/chat-rooms/proposals/*", "/api/chat-rooms/*/detail")
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .requestCache(cache -> cache.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new TokenAuthenticationFilter(users, mapper),
                        org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
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
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}
