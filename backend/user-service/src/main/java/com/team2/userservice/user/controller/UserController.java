package com.team2.userservice.user.controller;

import com.team2.userservice.user.dto.TokenReissueRequest;
import com.team2.userservice.user.dto.TokenResponse;
import com.team2.userservice.user.dto.UserLoginRequest;
import com.team2.userservice.user.dto.UserSignUpRequest;
import com.team2.userservice.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<Long> signUp(@RequestBody @Valid UserSignUpRequest request) {
        Long userId = userService.signUp(request);
        return ResponseEntity.ok(userId);
    }

    @PostMapping("/signin")
    public ResponseEntity<TokenResponse> signin(@RequestBody @Valid UserLoginRequest request) {
        TokenResponse tokenResponse = userService.signin(request);
        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(@RequestBody @Valid TokenReissueRequest request) {
        TokenResponse tokenResponse = userService.reissue(request);
        return ResponseEntity.ok(tokenResponse);
    }
}