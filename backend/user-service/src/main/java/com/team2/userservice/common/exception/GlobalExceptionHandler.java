package com.team2.userservice.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 프론트가 response.json()으로 일관되게 파싱할 수 있도록 항상 JSON으로 내려준다.
    // (예전엔 body가 순수 String이라 Spring이 text/plain으로 내려보내 프론트에서 "not valid JSON" 오류가 났음)
    public record ErrorResponse(String message) {}

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(new ErrorResponse(e.getErrorCode().getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getMessage()));
    }

    // 외부 API(Kakao Local 등) 호출 실패가 그대로 500 스택으로 나가지 않도록 처리
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ErrorResponse> handleRestClientException(RestClientException e) {
        log.error("외부 API 호출 실패", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("외부 서비스 호출에 실패했습니다. 잠시 후 다시 시도해주세요."));
    }
}
