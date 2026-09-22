package com.team2.chatservice.common.exception;

import com.team2.common.exception.ApiErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode implements ApiErrorCode {
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_ROOM_NOT_FOUND", "채팅방을 찾을 수 없습니다."),
    UNAUTHORIZED_CHAT_ROOM_ACCESS(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED_CHAT_ROOM_ACCESS", "채팅방 접근 권한이 없습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "유효하지 않은 입력값입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
