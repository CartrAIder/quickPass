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

    // 결제 시도 생성
    @PostMapping("/api/orders/{orderId}/payment-attempts")
    public ResponseEntity<PaymentAttemptCreateResponse> createAttempt(
            @AuthenticationPrincipal Long userId,
            @PathVariable String orderId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createAttempt(userId, orderId));
    }

    // 결제 승인 컨트롤러
    @PostMapping("/api/payments/confirm")
    public ResponseEntity<PaymentConfirmResponse> confirm(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PaymentConfirmRequest request
    ) {
        PaymentConfirmResponse response = paymentService.confirm(userId, request);
        HttpStatus status = response.status() == PaymentStatus.APPROVED ? HttpStatus.OK : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(response);
    }

    // 토스 클라이언트 키 제공 컨트롤러(공개키이므로 결제창 초기화를 위해 제공)
    @GetMapping("/api/payments/client-key")
    public TossClientKeyResponse clientKey() {
        return new TossClientKeyResponse(tossPaymentsProperties.clientKey());
    }

    public record TossClientKeyResponse(String clientKey) {
    }
}
