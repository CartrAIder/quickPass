package com.mart.quickpass.gate.exception;

import com.mart.quickpass.global.exception.BusinessException;
import com.mart.quickpass.global.exception.ErrorCode;

public class GateException extends BusinessException {
    public GateException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
