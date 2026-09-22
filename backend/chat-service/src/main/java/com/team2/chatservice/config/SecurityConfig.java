package com.team2.chatservice.config;

import com.team2.chatservice.client.UserClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean @Order(1)
    public SecurityFilterChain authenticatedChat(HttpSecurity http, UserClient users) throws Exception {
        return http.securityMatcher("/api/chat-rooms/**")
                .csrf(csrf -> csrf.disable()).formLogin(form -> form.disable()).httpBasic(basic -> basic.disable())
                .requestCache(cache -> cache.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new TokenAuthenticationFilter(users), AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated()).build();
    }

    @Bean @Order(2)
    public SecurityFilterChain otherChat(HttpSecurity http) throws Exception {
        // These controllers validate Bearer/internal headers; STOMP validates CONNECT and each operation.
        return http.csrf(csrf -> csrf.disable()).formLogin(form -> form.disable()).httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).build();
    }
}
