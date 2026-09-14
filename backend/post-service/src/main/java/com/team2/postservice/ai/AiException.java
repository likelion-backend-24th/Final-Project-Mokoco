package com.team2.postservice.ai;

import org.springframework.http.HttpStatus;

public class AiException extends RuntimeException {
    final HttpStatus status;
    final String code;
    public AiException(HttpStatus status, String code, String message) {
        super(message); this.status = status; this.code = code;
    }
    static AiException input(String message) { return new AiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", message); }
    static AiException output() { return new AiException(HttpStatus.BAD_GATEWAY, "INVALID_AI_RESPONSE", "AI 응답을 확인하지 못했습니다. 직접 작성하거나 다시 시도해주세요."); }
}
