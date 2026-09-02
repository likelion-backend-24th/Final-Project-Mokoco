package com.team2.userservice.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 500)
    private String token;

    @Builder
    public RefreshToken(String email, String token) {
        this.email = email;
        this.token = token;
    }

    public void updateToken(String newToken) {
        this.token = newToken;
    }
}