package com.team2.userservice.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TokenReissueRequest {

    @NotBlank(message = "Email cannot be blank")
    private String email;

    @NotBlank(message = "Refresh Token cannot be blank")
    private String refreshToken;
}