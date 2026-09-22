package com.team2.paymentservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class SecurityConfig {
    @Bean @Order(1)
    public SecurityFilterChain payments(HttpSecurity http,
            @Value("${services.user-service.url:http://localhost:8081}") String url,
            @Value("${internal.service-key}") String key) throws Exception {
        if (key.isBlank()) throw new IllegalArgumentException("INTERNAL_SERVICE_KEY must not be blank");
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        RestClient users = RestClient.builder().baseUrl(url).defaultHeader("X-Internal-Service-Key", key).requestFactory(factory).build();
        return http.securityMatcher(request -> request.getRequestURI().startsWith("/payments")
                        && !request.getRequestURI().equals("/payments/webhook"))
                .csrf(csrf -> csrf.disable()).formLogin(form -> form.disable()).httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new TokenAuthenticationFilter(users), AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated()).build();
    }
    @Bean @Order(2)
    public SecurityFilterChain internalAndWebhook(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable()).formLogin(form -> form.disable()).httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/payments/webhook", "/internal/payments/**", "/error").permitAll()
                        .anyRequest().denyAll()).build();
    }
}
