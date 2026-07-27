package com.mart.quickpass.global.exception;

import lombok.Getter;

/** HTTP API로 노출되는 업무 예외의 공통 기반 클래스다. */
@Getter
public abstract class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    protected BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
