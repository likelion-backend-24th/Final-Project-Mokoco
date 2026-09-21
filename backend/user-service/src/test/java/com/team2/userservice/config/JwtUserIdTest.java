package com.team2.userservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team2.userservice.common.exception.CustomException;
import com.team2.userservice.region.repository.RegionRepository;
import com.team2.userservice.user.controller.UserClientController;
import com.team2.userservice.user.dto.TokenReissueRequest;
import com.team2.userservice.user.entity.RefreshToken;
import com.team2.userservice.user.entity.Role;
import com.team2.userservice.user.entity.User;
import com.team2.userservice.user.repository.RefreshTokenRepository;
import com.team2.userservice.user.repository.UserRepository;
import com.team2.userservice.user.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtUserIdTest {
    static final String ACCESS_KEY = "test-access-key-".repeat(4);
    static final String REFRESH_KEY = "test-refresh-key-".repeat(4);
    final JwtTokenProvider tokens = new JwtTokenProvider(ACCESS_KEY, REFRESH_KEY, 60_000, 120_000);

    @Test void accessAndRefreshUseStringUserIdSubjects() {
        String access = tokens.createAccessToken(42L, "USER");
        String refresh = tokens.createRefreshToken(42L);
        assertThat(tokens.getUserIdFromAccessToken(access)).isEqualTo(42L);
        assertThat(tokens.getUserIdFromRefreshToken(refresh)).isEqualTo(42L);
        assertThat(Jwts.parser().verifyWith(Keys.hmacShaKeyFor(ACCESS_KEY.getBytes(StandardCharsets.UTF_8)))
                .build().parseSignedClaims(access).getPayload().getSubject()).isEqualTo("42");
        assertThat(tokens.validateAccessToken(refresh)).isFalse();
        assertThat(tokens.validateRefreshToken(access)).isFalse();
        assertThatThrownBy(() -> tokens.createAccessToken(null, "USER")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void rejectsLegacyInvalidExpiredAndTamperedTokens() {
        for (String subject : new String[]{"user@example.com", "0", "-1", "01", "9223372036854775808"}) {
            String token = Jwts.builder().subject(subject).expiration(new Date(System.currentTimeMillis() + 60_000))
                    .signWith(Keys.hmacShaKeyFor(ACCESS_KEY.getBytes(StandardCharsets.UTF_8))).compact();
            assertThat(tokens.validateAccessToken(token)).isFalse();
        }
        JwtTokenProvider expired = new JwtTokenProvider(ACCESS_KEY, REFRESH_KEY, -1_000, -1_000);
        assertThat(tokens.validateAccessToken(expired.createAccessToken(42L, "USER"))).isFalse();
        assertThat(tokens.validateAccessToken(tokens.createAccessToken(42L, "USER") + "x")).isFalse();
    }

    @Test void filterStoresIdAndVerifiedRole() throws Exception {
        UserRepository users = mock(UserRepository.class);
        User user = mock(User.class);
        when(user.getId()).thenReturn(42L);
        when(user.getRole()).thenReturn(Role.ADMIN);
        when(users.findById(42L)).thenReturn(Optional.of(user));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tokens.createAccessToken(42L, "USER"));
        try {
            new JwtAuthenticationFilter(tokens, users).doFilter(request, new MockHttpServletResponse(),
                    (req, res) -> assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                            .isEqualTo(new com.team2.common.security.LoginUser(42L, com.team2.common.security.Role.ADMIN)));
            verify(users).findById(42L);
            verify(users, never()).findByEmail(anyString());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test void internalVerificationLooksUpUserId() {
        UserService users = mock(UserService.class);
        UserClientController controller = new UserClientController(users, tokens);
        assertThat(controller.verifyToken(tokens.createAccessToken(42L, "USER")).getStatusCode().value()).isEqualTo(200);
        verify(users).findUserById(42L);
        assertThat(controller.verifyToken("invalid").getStatusCode().value()).isEqualTo(401);
    }

    @Test void reissueUsesVerifiedSubjectRegardlessOfContactEmail() throws Exception {
        UserRepository users = mock(UserRepository.class);
        RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
        User user = mock(User.class);
        when(user.getId()).thenReturn(42L);
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.getRole()).thenReturn(Role.USER);
        when(users.findById(42L)).thenReturn(Optional.of(user));
        String refresh = tokens.createRefreshToken(42L);
        when(refreshTokens.findByUserId(42L))
                .thenReturn(Optional.of(new RefreshToken(42L, refresh)));
        UserService service = new UserService(users, mock(PasswordEncoder.class), tokens, refreshTokens, mock(RegionRepository.class));
        ObjectMapper mapper = new ObjectMapper();
        TokenReissueRequest request = mapper.readValue("{\"refreshToken\":\"" + refresh + "\"}", TokenReissueRequest.class);
        com.team2.userservice.user.dto.TokenResponse response = service.reissue(request);
        assertThat(tokens.getUserIdFromAccessToken(response.getAccessToken())).isEqualTo(42L);
        TokenReissueRequest wrong = mapper.readValue("{\"email\":\"other@example.com\",\"refreshToken\":\"" + refresh + "\"}", TokenReissueRequest.class);
        assertThat(tokens.getUserIdFromAccessToken(service.reissue(wrong).getAccessToken())).isEqualTo(42L);
        verify(users, never()).findByEmail(anyString());
    }
}
