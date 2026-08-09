package com.mart.quickpass.global.exception;

public class PasswordResetTooFrequentException extends BusinessException {

    public PasswordResetTooFrequentException() {
        super(ErrorCode.PASSWORD_RESET_TOO_FREQUENT, "잠시 후 인증번호를 다시 요청해 주세요.");
    }
}
