package com.mart.quickpass.email.exception;

import com.mart.quickpass.global.exception.BusinessException;
import com.mart.quickpass.global.exception.ErrorCode;

public class EmailNotVerifiedException extends BusinessException {
    public EmailNotVerifiedException() {
        super(ErrorCode.EMAIL_NOT_VERIFIED, "이메일 인증이 필요하거나 인증이 만료되었습니다.");
    }
}
