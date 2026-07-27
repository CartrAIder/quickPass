package com.mart.quickpass.global.exception;

public class InvalidCredentialsException extends BusinessException {

    public InvalidCredentialsException() {
        super(ErrorCode.INVALID_CREDENTIALS, "이메일 또는 비밀번호가 일치하지 않습니다.");
    }
}
