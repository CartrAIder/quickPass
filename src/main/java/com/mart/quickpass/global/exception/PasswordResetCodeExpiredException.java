package com.mart.quickpass.global.exception;

public class PasswordResetCodeExpiredException extends BusinessException {

    public PasswordResetCodeExpiredException() {
        super(ErrorCode.PASSWORD_RESET_CODE_EXPIRED, "인증번호가 만료되었거나 존재하지 않습니다.");
    }
}
