package com.team2.postservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// chat-service가 post-service의 /api/internal/** 를 호출할 때 쓰는 서비스 간 인증.
// user-service의 InternalServiceSecurityConfig와 동일 패턴, 같은 INTERNAL_SERVICE_KEY 재사용.
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
