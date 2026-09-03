package com.team2.postservice.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Post
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글이 존재하지 않습니다."),
    UNAUTHORIZED_POST_UPDATE(HttpStatus.FORBIDDEN, "UNAUTHORIZED_POST_UPDATE", "작성자만 수정할 수 있습니다."),
    UNAUTHORIZED_POST_DELETE(HttpStatus.FORBIDDEN, "UNAUTHORIZED_POST_DELETE", "작성자만 삭제할 수 있습니다."),

    // Proposal
    POST_NOT_FOUND_FOR_PROPOSAL(HttpStatus.NOT_FOUND, "POST_NOT_FOUND_FOR_PROPOSAL", "해당 수리 요청글이 존재하지 않습니다."),
    PROPOSAL_NOT_FOUND(HttpStatus.NOT_FOUND, "PROPOSAL_NOT_FOUND", "해당 제안이 존재하지 않습니다."),
    UNAUTHORIZED_PROPOSAL_ADOPT(HttpStatus.FORBIDDEN, "UNAUTHORIZED_PROPOSAL_ADOPT", "수리 요청글 작성자만 제안을 채택할 수 있습니다."),
    UNAUTHORIZED_PROPOSAL_DELETE(HttpStatus.FORBIDDEN, "UNAUTHORIZED_PROPOSAL_DELETE", "작성자만 삭제할 수 있습니다."),

    // Common
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