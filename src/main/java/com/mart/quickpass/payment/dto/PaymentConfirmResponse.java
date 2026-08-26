package com.mart.quickpass.payment.dto;

import com.mart.quickpass.payment.entity.PaymentAttempt;
import com.mart.quickpass.payment.entity.PaymentStatus;

public record PaymentConfirmResponse(
        String paymentAttemptId,
        String paymentKey,
        Long approvedAmount,
        PaymentStatus status,
        String gateToken,
        String code,
        String message
) {

    public static PaymentConfirmResponse from(PaymentAttempt attempt, String gateToken) {
        return new PaymentConfirmResponse(
                attempt.getPaymentAttemptId(),
                attempt.getPaymentKey(),
                attempt.getApprovedAmount(),
                attempt.getStatus(),
                gateToken,
                attempt.getFailureCode(),
                attempt.getFailureMessage()
        );
    }

    public static PaymentConfirmResponse from(PaymentAttempt attempt) {
        return from(attempt, null);
    }
}
