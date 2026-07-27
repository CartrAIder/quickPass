package com.mart.quickpass.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record PaymentConfirmRequest(

        @NotBlank(message = "토스 결제 키는 필수입니다.")
        @Size(max = 200, message = "토스 결제 키는 200자를 초과할 수 없습니다.")
        String paymentKey,

        @NotBlank(message = "주문 ID는 필수입니다.")
        @Size(max = 64, message = "주문 ID는 64자를 초과할 수 없습니다.")
        String orderId,

        @NotNull(message = "결제 금액은 필수입니다.")
        @PositiveOrZero(message = "결제 금액은 0 이상이어야 합니다.")
        Long amount,

        @NotBlank(message = "결제 시도 ID는 필수입니다.")
        @Size(max = 64, message = "결제 시도 ID는 64자를 초과할 수 없습니다.")
        String paymentAttemptId
) {
}
