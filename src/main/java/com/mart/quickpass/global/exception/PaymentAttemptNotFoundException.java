package com.mart.quickpass.global.exception;

public class PaymentAttemptNotFoundException extends RuntimeException {

    public PaymentAttemptNotFoundException(String paymentAttemptId) {
        super("존재하지 않는 결제 시도입니다: " + paymentAttemptId);
    }
}
