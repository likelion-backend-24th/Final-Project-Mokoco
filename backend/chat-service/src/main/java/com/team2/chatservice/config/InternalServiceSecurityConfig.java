package com.team2.chatservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// post-service가 chat-service의 /api/internal/** 를 호출할 때 쓰는 서비스 간 인증.
@Configuration
public class InternalServiceSecurityConfig {
    @Bean
    @Order(0)
    public SecurityFilterChain internalServiceSecurityFilterChain(
            HttpSecurity http, @Value("${internal.service-key}") String key) throws Exception {
        return http.securityMatcher("/api/internal/**")
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().hasAuthority("INTERNAL_SERVICE"))
                .addFilterBefore(new InternalServiceAuthenticationFilter(key), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
