package com.mart.quickpass.global.exception;

public class CurrentPasswordMismatchException extends BusinessException {

    public CurrentPasswordMismatchException() {
        super(ErrorCode.CURRENT_PASSWORD_MISMATCH, "현재 비밀번호가 일치하지 않습니다.");
    }
}
