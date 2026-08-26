package com.mart.quickpass.global.exception;

public class PasswordResetAttemptsExceededException extends BusinessException {

    public PasswordResetAttemptsExceededException() {
        super(ErrorCode.PASSWORD_RESET_ATTEMPTS_EXCEEDED,
                "인증번호 입력 횟수를 초과했습니다. 인증번호를 다시 발급해 주세요.");
    }
}
