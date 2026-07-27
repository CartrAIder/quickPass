package com.mart.quickpass.payment.dto;

import com.mart.quickpass.payment.entity.PaymentAttempt;
import com.mart.quickpass.payment.entity.PaymentStatus;

public record PaymentConfirmResponse(
        String paymentAttemptId,
        String paymentKey,
        Long approvedAmount,
        PaymentStatus status,
        String code,
        String message
) {

    public static PaymentConfirmResponse from(PaymentAttempt attempt) {
        return new PaymentConfirmResponse(
                attempt.getPaymentAttemptId(),
                attempt.getPaymentKey(),
                attempt.getApprovedAmount(),
                attempt.getStatus(),
                attempt.getFailureCode(),
                attempt.getFailureMessage()
        );
    }
}
