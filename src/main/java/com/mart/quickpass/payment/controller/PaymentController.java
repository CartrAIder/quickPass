package com.mart.quickpass.payment.controller;

import com.mart.quickpass.payment.dto.PaymentAttemptCreateResponse;
import com.mart.quickpass.payment.dto.PaymentConfirmRequest;
import com.mart.quickpass.payment.dto.PaymentConfirmResponse;
import com.mart.quickpass.payment.entity.PaymentStatus;
import com.mart.quickpass.payment.service.PaymentService;
import com.mart.quickpass.global.config.TossPaymentsProperties;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final TossPaymentsProperties tossPaymentsProperties;

    @PostMapping("/api/orders/{orderId}/payment-attempts")
    public ResponseEntity<PaymentAttemptCreateResponse> createAttempt(
            @AuthenticationPrincipal Long userId,
            @PathVariable String orderId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createAttempt(userId, orderId));
    }

    @PostMapping("/api/payments/confirm")
    public ResponseEntity<PaymentConfirmResponse> confirm(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PaymentConfirmRequest request
    ) {
        PaymentConfirmResponse response = paymentService.confirm(userId, request);
        HttpStatus status = response.status() == PaymentStatus.APPROVED ? HttpStatus.OK : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(response);
    }

    /** 토스 클라이언트 키는 공개 키이므로 결제창 초기화용으로만 제공한다. */
    @GetMapping("/api/payments/client-key")
    public TossClientKeyResponse clientKey() {
        return new TossClientKeyResponse(tossPaymentsProperties.clientKey());
    }

    public record TossClientKeyResponse(String clientKey) {
    }
}
