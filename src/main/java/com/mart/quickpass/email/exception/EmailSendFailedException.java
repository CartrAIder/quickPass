package com.mart.quickpass.email.exception;

import com.mart.quickpass.global.exception.BusinessException;
import com.mart.quickpass.global.exception.ErrorCode;

public class EmailSendFailedException extends BusinessException {
    public EmailSendFailedException() {
        super(ErrorCode.EMAIL_SEND_FAILED, "인증 이메일 전송에 실패했습니다. 잠시 후 다시 시도해 주세요.");
    }
}
