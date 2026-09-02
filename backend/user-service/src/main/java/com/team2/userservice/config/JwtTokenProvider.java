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

    // Access Token 생성 (30분)
    public String createAccessToken(String email, String role) {
        return createToken(email, role, accessTokenValidity, accessSecretKey);
    }

    // Refresh Token 생성 (7일)
    public String createRefreshToken(String email) {
        return createToken(email, null, refreshTokenValidity, refreshSecretKey);
    }

    private String createToken(String email, String role, long validity, SecretKey key) {
        JwtBuilder builder = Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + validity))
                .signWith(key);

        if (role != null) {
            builder.claim("role", role);
        }

        return builder.compact();
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
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // Access Token에서 이메일 추출
    public String getEmailFromAccessToken(String token) {
        return getEmail(token, accessSecretKey);
    }

    // Refresh Token에서 이메일 추출
    public String getEmailFromRefreshToken(String token) {
        return getEmail(token, refreshSecretKey);
    }

    private String getEmail(String token, SecretKey key) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}