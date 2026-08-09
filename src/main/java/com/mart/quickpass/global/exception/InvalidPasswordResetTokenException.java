package com.mart.quickpass.global.exception;

public class InvalidPasswordResetTokenException extends BusinessException {

    public InvalidPasswordResetTokenException() {
        super(ErrorCode.INVALID_PASSWORD_RESET_TOKEN, "유효하지 않거나 만료된 비밀번호 재설정 토큰입니다.");
    }
}
