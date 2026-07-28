package com.mart.quickpass.email.exception;

import com.mart.quickpass.global.exception.BusinessException;
import com.mart.quickpass.global.exception.ErrorCode;

public class EmailVerificationTooFrequentException extends BusinessException {
    public EmailVerificationTooFrequentException() {
        super(ErrorCode.EMAIL_VERIFICATION_TOO_FREQUENT, "잠시 후 인증번호를 다시 요청해 주세요.");
    }
}
