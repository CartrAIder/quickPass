package com.mart.quickpass.global.exception;

public class InvalidPaymentStateException extends BusinessException {

    public InvalidPaymentStateException(String message) {
        super(ErrorCode.INVALID_PAYMENT_STATE, message);
    }
}
