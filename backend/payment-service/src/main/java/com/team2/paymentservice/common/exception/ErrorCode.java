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
    INVALID_PAYMENT_STATUS(HttpStatus.CONFLICT, "INVALID_PAYMENT_STATUS", "결제를 생성할 수 없는 거래 상태입니다."),

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
