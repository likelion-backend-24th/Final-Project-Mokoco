package com.team2.common.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
    private final ApiErrorCode errorCode;

    public CustomException(ApiErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
