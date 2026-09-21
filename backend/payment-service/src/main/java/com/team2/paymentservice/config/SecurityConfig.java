package com.team2.paymentservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // post-service가 결제 상태 조회/정산을 요청할 때 쓰는 서비스 간 인증. user/post/chat-service와
    // 동일한 X-Internal-Service-Key 패턴.
    @Bean
    @Order(0)
    public SecurityFilterChain internalServiceSecurityFilterChain(
            HttpSecurity http, @Value("${internal.service-key}") String key) throws Exception {
        return http.securityMatcher("/internal/**")
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().hasAuthority("INTERNAL_SERVICE"))
                .addFilterBefore(new InternalServiceAuthenticationFilter(key), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // PortOne이 서명 헤더와 함께 직접 호출하는 웹훅 — 유저 로그인이 아니라 PaymentWebhookController의
    // 자체 서명 검증으로 인증한다. /payments/** 전체를 Bearer 필수로 묶기 전에(Order 1) 먼저
    // 매치시켜 빠져나가게 한다.
    @Bean
    @Order(1)
    public SecurityFilterChain webhookSecurityFilterChain(HttpSecurity http) throws Exception {
        return http.securityMatcher("/payments/webhook")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    // payments의 사용자 대면 엔드포인트(prepare/confirm/mine/단건 조회 등) 전부 Authorization: Bearer로
    // 직접 인증한다. post/chat-service와 동일한 패턴 — 더 이상 X-User-Email을 신뢰하지 않는다.
    @Bean
    @Order(2)
    public SecurityFilterChain authenticatedApiSecurityFilterChain(HttpSecurity http,
            com.team2.paymentservice.client.UserClient users, com.fasterxml.jackson.databind.ObjectMapper mapper) throws Exception {
        return http
                .securityMatcher("/payments/**")
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .requestCache(cache -> cache.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new TokenAuthenticationFilter(users, mapper), AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}
