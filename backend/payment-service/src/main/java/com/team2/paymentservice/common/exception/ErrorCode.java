package com.team2.paymentservice.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Payment
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "결제 정보가 존재하지 않습니다."),
    UNAUTHORIZED_PAYMENT_ACCESS(HttpStatus.FORBIDDEN, "UNAUTHORIZED_PAYMENT_ACCESS", "본인의 결제만 조회할 수 있습니다."),
    UNAUTHORIZED_PAYMENT_CREATE(HttpStatus.FORBIDDEN, "UNAUTHORIZED_PAYMENT_CREATE", "의뢰자 본인만 결제를 생성할 수 있습니다."),
    DUPLICATE_PAYMENT(HttpStatus.CONFLICT, "DUPLICATE_PAYMENT", "이미 결제가 진행 중이거나 완료된 거래입니다."),
    POST_NOT_FOUND_FOR_PAYMENT(HttpStatus.NOT_FOUND, "POST_NOT_FOUND_FOR_PAYMENT", "결제 대상 게시글이 존재하지 않습니다."),
    POST_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "POST_SERVICE_UNAVAILABLE", "게시글 정보를 확인할 수 없습니다. 잠시 후 다시 시도해주세요."),
    INVALID_PAYMENT_STATUS(HttpStatus.CONFLICT, "INVALID_PAYMENT_STATUS", "결제를 생성할 수 없는 거래 상태입니다."),
    PAYMENT_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "PAYMENT_VERIFICATION_FAILED", "결제 내역을 확인할 수 없습니다."),
    PAYMENT_NOT_PAID(HttpStatus.CONFLICT, "PAYMENT_NOT_PAID", "PortOne에서 결제 완료가 확인되지 않았습니다."),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.CONFLICT, "PAYMENT_AMOUNT_MISMATCH", "결제 금액이 일치하지 않습니다."),
    PORTONE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "PORTONE_UNAVAILABLE", "결제 서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요."),
    INVALID_WEBHOOK_SIGNATURE(HttpStatus.UNAUTHORIZED, "INVALID_WEBHOOK_SIGNATURE", "웹훅 서명 검증에 실패했습니다."),

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
