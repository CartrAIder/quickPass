package com.mart.quickpass.email.exception;

import com.mart.quickpass.global.exception.BusinessException;
import com.mart.quickpass.global.exception.ErrorCode;

public class InvalidEmailVerificationCodeException extends BusinessException {
    public InvalidEmailVerificationCodeException() {
        super(ErrorCode.INVALID_EMAIL_VERIFICATION_CODE, "인증번호가 일치하지 않습니다.");
    }
}
