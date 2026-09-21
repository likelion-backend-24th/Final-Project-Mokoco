package com.team2.common.exception;

import org.springframework.http.HttpStatus;

public interface ApiErrorCode {
    HttpStatus getHttpStatus();
    String getCode();
    String getMessage();
}
