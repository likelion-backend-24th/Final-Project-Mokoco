package com.team2.postservice.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Post
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글이 존재하지 않습니다."),
    UNAUTHORIZED_POST_UPDATE(HttpStatus.FORBIDDEN, "UNAUTHORIZED_POST_UPDATE", "작성자만 수정할 수 있습니다."),
    UNAUTHORIZED_POST_DELETE(HttpStatus.FORBIDDEN, "UNAUTHORIZED_POST_DELETE", "작성자만 삭제할 수 있습니다."),

    // Post Image
    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "IMAGE_NOT_FOUND", "이미지가 존재하지 않습니다."),
    INVALID_IMAGE_FILE(HttpStatus.BAD_REQUEST, "INVALID_IMAGE_FILE", "유효하지 않은 이미지 파일입니다."),
    IMAGE_FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "IMAGE_FILE_TOO_LARGE", "이미지 파일 용량은 10MB를 초과할 수 없습니다."),
    TOO_MANY_IMAGES(HttpStatus.BAD_REQUEST, "TOO_MANY_IMAGES", "게시글당 이미지는 최대 5장까지 등록할 수 있습니다."),
    IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE_UPLOAD_FAILED", "이미지 업로드에 실패했습니다."),

    // Proposal
    POST_NOT_FOUND_FOR_PROPOSAL(HttpStatus.NOT_FOUND, "POST_NOT_FOUND_FOR_PROPOSAL", "해당 수리 요청글이 존재하지 않습니다."),
    PROPOSAL_NOT_FOUND(HttpStatus.NOT_FOUND, "PROPOSAL_NOT_FOUND", "해당 제안이 존재하지 않습니다."),
    UNAUTHORIZED_PROPOSAL_ADOPT(HttpStatus.FORBIDDEN, "UNAUTHORIZED_PROPOSAL_ADOPT", "작성자만 제안을 채택할 수 있습니다."),

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