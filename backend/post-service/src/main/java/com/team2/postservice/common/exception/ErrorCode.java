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
    UNAUTHORIZED_PROPOSAL_ADOPT(HttpStatus.FORBIDDEN, "UNAUTHORIZED_PROPOSAL_ADOPT", "수리 요청글 작성자만 제안을 채택할 수 있습니다."),
    UNAUTHORIZED_PROPOSAL_DELETE(HttpStatus.FORBIDDEN, "UNAUTHORIZED_PROPOSAL_DELETE", "작성자만 삭제할 수 있습니다."),

    // FixDeal
    FIX_DEAL_NOT_FOUND(HttpStatus.NOT_FOUND, "FIX_DEAL_NOT_FOUND", "해당 수리거래 내역을 찾을 수 없습니다."),
    UNAUTHORIZED_FIX_DEAL_ACTION(HttpStatus.FORBIDDEN, "UNAUTHORIZED_FIX_DEAL_ACTION", "본인이 참여한 거래만 처리할 수 있습니다."),
    INVALID_FIX_DEAL_STATUS(HttpStatus.CONFLICT, "INVALID_FIX_DEAL_STATUS", "현재 거래 상태에서는 처리할 수 없습니다."),
    PAYMENT_NOT_COMPLETED(HttpStatus.CONFLICT, "PAYMENT_NOT_COMPLETED", "결제가 완료되지 않아 수리 완료를 수락할 수 없습니다."),
    PAYMENT_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_SERVICE_UNAVAILABLE", "결제 정보를 확인할 수 없습니다. 잠시 후 다시 시도해주세요."),

    // ChatRoom
    UNAUTHORIZED_CHAT_ROOM_CREATE(HttpStatus.FORBIDDEN, "UNAUTHORIZED_CHAT_ROOM_CREATE", "수리 요청글 작성자만 채팅방을 만들 수 있습니다."),
    CHAT_ROOM_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "CHAT_ROOM_NOT_AVAILABLE", "채택된 제안이 아닙니다."),
    CHAT_ROOM_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "CHAT_ROOM_ALREADY_EXISTS", "이미 생성된 채팅방입니다."),
    UNAUTHORIZED_CHAT_ROOM_ACCESS(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED_CHAT_ROOM_ACCESS", "채팅방 접근 권한이 없습니다."),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_ROOM_NOT_FOUND", "채팅방을 찾을 수 없습니다."),

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