package com.mart.quickpass.global.exception;

public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException() {
        super("유효하지 않은 리프레시 토큰입니다.");
    }

    public InvalidTokenException(String message) {
        super(message);
    }
}
