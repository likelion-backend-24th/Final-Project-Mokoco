package com.team2.userservice.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TokenReissueRequest {

    // Legacy clients may send contact email; authentication uses only the refresh token.
    private String email;

    @NotBlank(message = "Refresh Token cannot be blank")
    private String refreshToken;
}