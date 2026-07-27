package com.mart.quickpass.payment.dto;

import com.mart.quickpass.payment.entity.PaymentAttempt;
import com.mart.quickpass.payment.entity.PaymentStatus;

public record PaymentAttemptCreateResponse(
        String paymentAttemptId,
        String orderId,
        String orderName,
        Long amount,
        String provider,
        PaymentStatus status
) {

    public static PaymentAttemptCreateResponse from(PaymentAttempt attempt) {
        return new PaymentAttemptCreateResponse(
                attempt.getPaymentAttemptId(),
                attempt.getOrder().getOrderId(),
                attempt.getOrder().getOrderName(),
                attempt.getRequestedAmount(),
                attempt.getProvider(),
                attempt.getStatus()
        );
    }
}
