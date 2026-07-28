package com.mart.quickpass.email.exception;

import com.mart.quickpass.global.exception.BusinessException;
import com.mart.quickpass.global.exception.ErrorCode;

public class EmailVerificationExpiredException extends BusinessException {
    public EmailVerificationExpiredException() {
        super(ErrorCode.EMAIL_VERIFICATION_EXPIRED, "인증번호가 만료되었거나 존재하지 않습니다.");
    }
}
