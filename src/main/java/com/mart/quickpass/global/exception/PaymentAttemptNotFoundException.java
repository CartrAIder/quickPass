package com.mart.quickpass.global.exception;

public class PaymentAttemptNotFoundException extends BusinessException {

    public PaymentAttemptNotFoundException(String paymentAttemptId) {
        super(ErrorCode.PAYMENT_ATTEMPT_NOT_FOUND, "존재하지 않는 결제 시도입니다: " + paymentAttemptId);
    }
}
