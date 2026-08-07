package com.mart.quickpass.global.exception;

public class InvalidPasswordResetCodeException extends BusinessException {

    public InvalidPasswordResetCodeException() {
        super(ErrorCode.INVALID_PASSWORD_RESET_CODE, "인증번호가 일치하지 않습니다.");
    }
}
