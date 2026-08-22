package com.mart.quickpass.payment.controller;

import com.mart.quickpass.global.config.TossPaymentsProperties;
import com.mart.quickpass.payment.dto.PaymentAttemptCreateResponse;
import com.mart.quickpass.payment.dto.PaymentAttemptCreateResult;
import com.mart.quickpass.payment.entity.PaymentStatus;
import com.mart.quickpass.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentControllerTest {

    private final PaymentService paymentService = mock(PaymentService.class);
    private final PaymentController controller = new PaymentController(
            paymentService, mock(TossPaymentsProperties.class));
    private final PaymentAttemptCreateResponse response = new PaymentAttemptCreateResponse(
            "attempt-1", "order-1", "우유", 3000L, "TOSS_PAYMENTS", PaymentStatus.READY);

    @Test
    void newlyCreatedAttemptReturnsCreated() {
        when(paymentService.createAttempt(1L, "order-1"))
                .thenReturn(PaymentAttemptCreateResult.created(response));

        var result = controller.createAttempt(1L, "order-1");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    void existingActiveAttemptReturnsOk() {
        when(paymentService.createAttempt(1L, "order-1"))
                .thenReturn(PaymentAttemptCreateResult.existing(response));

        var result = controller.createAttempt(1L, "order-1");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }
}
