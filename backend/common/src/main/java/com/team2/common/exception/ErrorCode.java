package com.team2.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // ===== user-service =====
    DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "DUPLICATE_EMAIL", "이미 존재하는 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.BAD_REQUEST, "DUPLICATE_NICKNAME", "이미 사용중인 닉네임입니다."),
    USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "USER_NOT_FOUND", "존재하지 않는 회원입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD", "비밀번호가 일치하지 않습니다."),
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED", "정지된 계정입니다. 고객센터에 문의해주세요."),
    FORBIDDEN_NOT_ADMIN(HttpStatus.FORBIDDEN, "FORBIDDEN_NOT_ADMIN", "관리자만 접근할 수 있습니다."),
    CANNOT_MODIFY_SELF(HttpStatus.FORBIDDEN, "CANNOT_MODIFY_SELF", "본인 계정의 권한·상태는 여기서 변경할 수 없습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.BAD_REQUEST, "INVALID_REFRESH_TOKEN", "유효하지 않은 Refresh Token입니다."),
    EXPIRED_SESSION(HttpStatus.BAD_REQUEST, "EXPIRED_SESSION", "존재하지 않거나 만료된 세션입니다. 다시 로그인해주세요."),
    INVALID_TOKEN_VALUE(HttpStatus.BAD_REQUEST, "INVALID_TOKEN_VALUE", "토큰 정보가 일치하지 않습니다."),
    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "REGION_NOT_FOUND", "지역 정보를 찾을 수 없습니다."),
    REGION_LOOKUP_FAILED(HttpStatus.BAD_GATEWAY, "REGION_LOOKUP_FAILED", "지역 정보 조회에 실패했습니다. 잠시 후 다시 시도해주세요."),

    // ===== post-service: Post =====
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "로그인이 필요합니다."),
    ACTIVITY_REGION_REQUIRED(HttpStatus.CONFLICT, "ACTIVITY_REGION_REQUIRED", "활동 지역을 먼저 설정해주세요."),
    USER_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "USER_SERVICE_UNAVAILABLE", "사용자 또는 지역 정보를 확인할 수 없습니다."),
    POST_NOT_ACCEPTING_PROPOSALS(HttpStatus.CONFLICT, "POST_NOT_ACCEPTING_PROPOSALS", "제안을 받을 수 없는 요청입니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글이 존재하지 않습니다."),
    UNAUTHORIZED_POST_UPDATE(HttpStatus.FORBIDDEN, "UNAUTHORIZED_POST_UPDATE", "작성자만 수정할 수 있습니다."),
    UNAUTHORIZED_POST_DELETE(HttpStatus.FORBIDDEN, "UNAUTHORIZED_POST_DELETE", "작성자만 삭제할 수 있습니다."),
    POST_HAS_ACTIVE_DEAL(HttpStatus.CONFLICT, "POST_HAS_ACTIVE_DEAL", "진행 중인 거래가 있는 글은 삭제할 수 없습니다."),

    // ===== post-service: Post Image =====
    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "IMAGE_NOT_FOUND", "이미지가 존재하지 않습니다."),
    INVALID_IMAGE_FILE(HttpStatus.BAD_REQUEST, "INVALID_IMAGE_FILE", "유효하지 않은 이미지 파일입니다."),
    IMAGE_FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "IMAGE_FILE_TOO_LARGE", "이미지 파일 용량은 10MB를 초과할 수 없습니다."),
    TOO_MANY_IMAGES(HttpStatus.BAD_REQUEST, "TOO_MANY_IMAGES", "게시글당 이미지는 최대 5장까지 등록할 수 있습니다."),
    IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE_UPLOAD_FAILED", "이미지 업로드에 실패했습니다."),

    // ===== post-service: Proposal =====
    POST_NOT_FOUND_FOR_PROPOSAL(HttpStatus.NOT_FOUND, "POST_NOT_FOUND_FOR_PROPOSAL", "해당 수리 요청글이 존재하지 않습니다."),
    PROPOSAL_NOT_FOUND(HttpStatus.NOT_FOUND, "PROPOSAL_NOT_FOUND", "해당 제안이 존재하지 않습니다."),
    UNAUTHORIZED_PROPOSAL_ADOPT(HttpStatus.FORBIDDEN, "UNAUTHORIZED_PROPOSAL_ADOPT", "수리 요청글 작성자만 제안을 채택할 수 있습니다."),
    UNAUTHORIZED_PROPOSAL_DELETE(HttpStatus.FORBIDDEN, "UNAUTHORIZED_PROPOSAL_DELETE", "작성자만 삭제할 수 있습니다."),
    PROPOSAL_ADOPTION_ALREADY_PAID(HttpStatus.CONFLICT, "PROPOSAL_ADOPTION_ALREADY_PAID", "결제가 완료된 거래는 채택을 취소할 수 없습니다."),

    // ===== post-service: FixDeal =====
    FIX_DEAL_NOT_FOUND(HttpStatus.NOT_FOUND, "FIX_DEAL_NOT_FOUND", "해당 수리거래 내역을 찾을 수 없습니다."),
    UNAUTHORIZED_FIX_DEAL_ACTION(HttpStatus.FORBIDDEN, "UNAUTHORIZED_FIX_DEAL_ACTION", "본인이 참여한 거래만 처리할 수 있습니다."),
    INVALID_FIX_DEAL_STATUS(HttpStatus.CONFLICT, "INVALID_FIX_DEAL_STATUS", "현재 거래 상태에서는 처리할 수 없습니다."),
    PAYMENT_NOT_COMPLETED(HttpStatus.CONFLICT, "PAYMENT_NOT_COMPLETED", "결제가 완료되지 않아 수리 완료를 수락할 수 없습니다."),
    PAYMENT_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_SERVICE_UNAVAILABLE", "결제 정보를 확인할 수 없습니다. 잠시 후 다시 시도해주세요."),

    // ===== post-service: ChatRoom =====
    UNAUTHORIZED_CHAT_ROOM_CREATE(HttpStatus.FORBIDDEN, "UNAUTHORIZED_CHAT_ROOM_CREATE", "수리 요청글 작성자만 채팅방을 만들 수 있습니다."),
    CHAT_ROOM_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "CHAT_ROOM_NOT_AVAILABLE", "채택된 제안이 아닙니다."),
    CHAT_ROOM_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "CHAT_ROOM_ALREADY_EXISTS", "이미 생성된 채팅방입니다."),
    UNAUTHORIZED_CHAT_ROOM_ACCESS(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED_CHAT_ROOM_ACCESS", "채팅방 접근 권한이 없습니다."),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_ROOM_NOT_FOUND", "채팅방을 찾을 수 없습니다."),

    // ===== post-service: Notification =====
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "알림이 존재하지 않습니다."),
    UNAUTHORIZED_NOTIFICATION_ACCESS(HttpStatus.FORBIDDEN, "UNAUTHORIZED_NOTIFICATION_ACCESS", "본인의 알림만 확인할 수 있습니다."),

    // ===== post-service: Admin =====
    UNAUTHORIZED_ADMIN_ACTION(HttpStatus.FORBIDDEN, "UNAUTHORIZED_ADMIN_ACTION", "관리자만 접근할 수 있습니다."),

    // ===== post-service: Report =====
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT_NOT_FOUND", "신고 내역이 존재하지 않습니다."),
    INVALID_REPORT_TARGET(HttpStatus.BAD_REQUEST, "INVALID_REPORT_TARGET", "신고 대상 정보가 올바르지 않습니다."),

    // ===== post-service: Review =====
    FIX_DEAL_NOT_FOUND_FOR_REVIEW(HttpStatus.NOT_FOUND, "FIX_DEAL_NOT_FOUND_FOR_REVIEW", "후기를 작성할 거래를 찾을 수 없습니다."),
    UNAUTHORIZED_REVIEW_CREATE(HttpStatus.FORBIDDEN, "UNAUTHORIZED_REVIEW_CREATE", "의뢰자 본인만 후기를 작성할 수 있습니다."),
    TRANSACTION_NOT_COMPLETED(HttpStatus.CONFLICT, "TRANSACTION_NOT_COMPLETED", "완료된 거래에만 후기를 작성할 수 있습니다."),
    REVIEW_DEADLINE_EXPIRED(HttpStatus.CONFLICT, "REVIEW_DEADLINE_EXPIRED", "후기 작성 기한(완료 후 3일)이 지났습니다."),
    DUPLICATE_REVIEW(HttpStatus.CONFLICT, "DUPLICATE_REVIEW", "이미 후기를 작성한 거래입니다."),
    TOO_MANY_REVIEW_IMAGES(HttpStatus.BAD_REQUEST, "TOO_MANY_REVIEW_IMAGES", "후기당 이미지는 최대 5장까지 등록할 수 있습니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND", "후기를 찾을 수 없습니다."),
    INVALID_RATING(HttpStatus.BAD_REQUEST, "INVALID_RATING", "평점은 1~5 사이의 정수여야 합니다."),

    // ===== post-service: Resume =====
    DUPLICATE_RESUME(HttpStatus.CONFLICT, "DUPLICATE_RESUME", "이미 작성된 이력서가 있습니다. 수정을 이용해주세요."),
    RESUME_NOT_FOUND(HttpStatus.NOT_FOUND, "RESUME_NOT_FOUND", "이력서를 찾을 수 없습니다."),

    // ===== payment-service =====
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "결제 정보가 존재하지 않습니다."),
    UNAUTHORIZED_PAYMENT_ACCESS(HttpStatus.FORBIDDEN, "UNAUTHORIZED_PAYMENT_ACCESS", "본인의 결제만 조회할 수 있습니다."),
    UNAUTHORIZED_PAYMENT_CREATE(HttpStatus.FORBIDDEN, "UNAUTHORIZED_PAYMENT_CREATE", "의뢰자 본인만 결제를 생성할 수 있습니다."),
    DUPLICATE_PAYMENT(HttpStatus.CONFLICT, "DUPLICATE_PAYMENT", "이미 결제가 진행 중이거나 완료된 거래입니다."),
    POST_NOT_FOUND_FOR_PAYMENT(HttpStatus.NOT_FOUND, "POST_NOT_FOUND_FOR_PAYMENT", "결제 대상 게시글이 존재하지 않습니다."),
    POST_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "POST_SERVICE_UNAVAILABLE", "게시글 정보를 확인할 수 없습니다. 잠시 후 다시 시도해주세요."),
    INVALID_PAYMENT_STATUS(HttpStatus.CONFLICT, "INVALID_PAYMENT_STATUS", "이미 종료된 거래는 결제할 수 없습니다."),
    PAYMENT_NOT_SETTLEABLE(HttpStatus.CONFLICT, "PAYMENT_NOT_SETTLEABLE", "완료되지 않은 결제는 정산할 수 없습니다."),
    PAYMENT_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "PAYMENT_VERIFICATION_FAILED", "결제 내역을 확인할 수 없습니다."),
    PAYMENT_NOT_PAID(HttpStatus.CONFLICT, "PAYMENT_NOT_PAID", "PortOne에서 결제 완료가 확인되지 않았습니다."),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.CONFLICT, "PAYMENT_AMOUNT_MISMATCH", "결제 금액이 일치하지 않습니다."),
    PORTONE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "PORTONE_UNAVAILABLE", "결제 서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요."),
    INVALID_WEBHOOK_SIGNATURE(HttpStatus.UNAUTHORIZED, "INVALID_WEBHOOK_SIGNATURE", "웹훅 서명 검증에 실패했습니다."),

    // ===== 공통 (post-service·payment-service 둘 다 쓰던 것 — 이름·의미 같아서 하나로 합침) =====
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
