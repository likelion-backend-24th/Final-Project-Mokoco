package com.team2.chatservice.common.exception;

import com.team2.common.exception.ApiErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode implements ApiErrorCode {
    UNAUTHORIZED_CHAT_ROOM_CREATE(HttpStatus.FORBIDDEN, "UNAUTHORIZED_CHAT_ROOM_CREATE", "수리 요청글 작성자만 채팅방을 만들 수 있습니다."),
    CHAT_ROOM_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "CHAT_ROOM_NOT_AVAILABLE", "채택된 제안이 아닙니다."),
    CHAT_ROOM_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "CHAT_ROOM_ALREADY_EXISTS", "이미 생성된 채팅방입니다."),
    UNAUTHORIZED_CHAT_ROOM_ACCESS(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED_CHAT_ROOM_ACCESS", "채팅방 접근 권한이 없습니다."),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_ROOM_NOT_FOUND", "채팅방을 찾을 수 없습니다."),
    FIX_DEAL_NOT_FOUND(HttpStatus.NOT_FOUND, "FIX_DEAL_NOT_FOUND", "해당 수리거래 내역을 찾을 수 없습니다."),
    PROPOSAL_NOT_FOUND(HttpStatus.NOT_FOUND, "PROPOSAL_NOT_FOUND", "해당 제안이 존재하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
