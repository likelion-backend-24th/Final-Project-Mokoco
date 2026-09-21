package com.team2.userservice.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey accessSecretKey;
    private final SecretKey refreshSecretKey;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;

    public JwtTokenProvider(
            @Value("${jwt.secretKey}") String secretKey,
            @Value("${jwt.refreshKey}") String refreshKey,
            @Value("${jwt.access-expiration-ms}") long accessTokenValidity,
            @Value("${jwt.refresh-expiration-ms}") long refreshTokenValidity) {
        this.accessSecretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.refreshSecretKey = Keys.hmacShaKeyFor(refreshKey.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
    }

    private String subject(Long userId) {
        if (userId == null || userId <= 0) throw new IllegalArgumentException("User ID must be positive");
        return userId.toString();
    }

    // Access Token 생성 (사용자 ID와 권한 포함)
    public String createAccessToken(Long userId, String role) {
        JwtBuilder builder = Jwts.builder()
                .subject(subject(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenValidity))
                .signWith(accessSecretKey);

        if (role != null) {
            builder.claim("role", role);
        }

        return builder.compact();
    }

    // Refresh Token 생성 (7일)
    public String createRefreshToken(Long userId) {
        return Jwts.builder()
                .subject(subject(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenValidity))
                .signWith(refreshSecretKey)
                .compact();
    }

    // Access Token 검증
    public boolean validateAccessToken(String token) {
        return validateToken(token, accessSecretKey);
    }

    // Refresh Token 검증
    public boolean validateRefreshToken(String token) {
        return validateToken(token, refreshSecretKey);
    }

    private boolean validateToken(String token, SecretKey key) {
        try {
            getUserId(token, key);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // Access Token에서 사용자 ID 추출
    public Long getUserIdFromAccessToken(String token) {
        return getUserId(token, accessSecretKey);
    }

    // Refresh Token에서 사용자 ID 추출
    public Long getUserIdFromRefreshToken(String token) {
        return getUserId(token, refreshSecretKey);
    }

    private Long getUserId(String token, SecretKey key) {
        String value = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        try {
            Long userId = Long.valueOf(value);
            if (!subject(userId).equals(value)) throw new IllegalArgumentException("Invalid user ID");
            return userId;
        } catch (IllegalArgumentException e) {
            throw new MalformedJwtException("JWT subject must be a positive user ID", e);
        }
    }
}