package com.team2.postservice.config;

import com.team2.postservice.common.security.TokenAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 글 목록 GET(/posts)만 GET/POST가 같은 경로를 쓰면서 GET은 비로그인도 봐야 해서 메서드까지
    // 명시해야 한다. 나머지(POST /posts, PATCH·DELETE /posts/{id}, /posts/{id}/proposals 등)는
    // 같은 경로에 공개 GET이 섞여있는 경우만 메서드를 명시하고, 그 외에는 경로만으로 충분하다.
    private static RequestMatcher matchers(RequestMatcher... matchers) {
        return new OrRequestMatcher(matchers);
    }
    private static AntPathRequestMatcher m(String pattern) { return new AntPathRequestMatcher(pattern); }
    private static AntPathRequestMatcher m(HttpMethod method, String pattern) { return new AntPathRequestMatcher(pattern, method.name()); }

    // post-service의 사용자 대면 엔드포인트 대부분이 여기 걸린다. 글/후기/관리자 액션/거래/알림/
    // 프로필/제안/신고/이력서/AI/계약서 — 전부 Authorization: Bearer로 직접 인증한다.
    // 공개 조회(글 상세, 글 목록 GET, 제안 목록 GET, 이력서 이메일 조회, 프로필 이메일 조회,
    // 게시글 기준 거래상태 조회)는 이 매처에서 빠져서 permitAll 체인으로 떨어진다.
    @Bean
    @Order(1)
    public SecurityFilterChain authenticatedApiSecurityFilterChain(HttpSecurity http,
            com.team2.postservice.client.UserClient users, com.fasterxml.jackson.databind.ObjectMapper mapper) throws Exception {
        return http
                .securityMatcher(matchers(
                        m("/api/ai/**"), m("/api/chat-rooms/*/contract/**"),
                        m("/api/chat-rooms/proposals/**"), m("/api/chat-rooms/fix-deals/**"),
                        m("/api/chat-rooms/*/detail"),
                        m(HttpMethod.POST, "/posts"), m("/posts/*/visibility"),
                        m(HttpMethod.PATCH, "/posts/*"), m(HttpMethod.DELETE, "/posts/*"),
                        m("/posts/*/images"), m("/posts/*/images/*"),
                        m(HttpMethod.POST, "/reviews"),
                        m("/api/admin/posts/**"), m("/api/admin/reports/**"), m("/api/admin/deals/**"), m("/api/admin/overview/**"),
                        m("/fix-deals/*"),
                        m("/notifications/**"),
                        m("/profile/transactions"), m("/profile/reviews"),
                        m(HttpMethod.POST, "/posts/*/proposals"),
                        m("/posts/*/proposals/*/adopt"), m("/posts/*/proposals/*/cancel"),
                        m(HttpMethod.DELETE, "/posts/*/proposals/*"),
                        m(HttpMethod.POST, "/api/reports"),
                        m(HttpMethod.POST, "/resumes"), m(HttpMethod.PATCH, "/resumes"),
                        m(HttpMethod.DELETE, "/resumes"), m("/resumes/me")
                ))
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .requestCache(cache -> cache.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new TokenAuthenticationFilter(users, mapper), AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .build();
    }

    // 글 목록(GET /posts)만 비로그인도 봐야 하는데 로그인했으면 지역 필터/닉네임 등을 개인화해서
    // 보여준다 — 토큰이 없거나 잘못돼도 막지 않고 LoginUser만 비워서(null) 통과시킨다.
    @Bean
    @Order(2)
    public SecurityFilterChain optionalAuthSecurityFilterChain(HttpSecurity http,
            com.team2.postservice.client.UserClient users, com.fasterxml.jackson.databind.ObjectMapper mapper) throws Exception {
        return http
                .securityMatcher(m(HttpMethod.GET, "/posts"))
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .requestCache(cache -> cache.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new TokenAuthenticationFilter(users, mapper, true), AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
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
                        .anyRequest().permitAll() // 나머지는 공개 조회 엔드포인트
                );

        return http.build();
    }
}
