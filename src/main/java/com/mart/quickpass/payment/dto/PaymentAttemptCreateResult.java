package com.mart.quickpass.payment.dto;

public record PaymentAttemptCreateResult(
        PaymentAttemptCreateResponse response,
        boolean created
) {

    public static PaymentAttemptCreateResult created(PaymentAttemptCreateResponse response) {
        return new PaymentAttemptCreateResult(response, true);
    }

    public static PaymentAttemptCreateResult existing(PaymentAttemptCreateResponse response) {
        return new PaymentAttemptCreateResult(response, false);
    }
}
