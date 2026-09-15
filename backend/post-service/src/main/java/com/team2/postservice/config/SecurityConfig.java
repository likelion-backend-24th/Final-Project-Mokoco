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

    @Bean
    @org.springframework.core.annotation.Order(1)
    public SecurityFilterChain aiSecurityFilterChain(HttpSecurity http,
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
