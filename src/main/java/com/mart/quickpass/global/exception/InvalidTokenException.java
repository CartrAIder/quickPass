package com.mart.quickpass.global.exception;

public class InvalidTokenException extends BusinessException {

    public InvalidTokenException() {
        super(ErrorCode.INVALID_REFRESH_TOKEN, "유효하지 않은 리프레시 토큰입니다.");
    }

    public InvalidTokenException(String message) {
        super(ErrorCode.INVALID_REFRESH_TOKEN, message);
    }
}
