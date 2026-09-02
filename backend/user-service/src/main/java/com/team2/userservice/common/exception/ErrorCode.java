package com.team2.userservice.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // User
    DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "이미 존재하는 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 회원입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),

    // Token / Auth
    INVALID_REFRESH_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 Refresh Token입니다."),
    EXPIRED_SESSION(HttpStatus.BAD_REQUEST, "존재하지 않거나 만료된 세션입니다. 다시 로그인해주세요."),
    INVALID_TOKEN_VALUE(HttpStatus.BAD_REQUEST, "토큰 정보가 일치하지 않습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}