package com.team2.userservice.common.exception;

import com.team2.common.exception.ApiErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode implements ApiErrorCode {
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
    REGION_LOOKUP_FAILED(HttpStatus.BAD_GATEWAY, "REGION_LOOKUP_FAILED", "지역 정보 조회에 실패했습니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
