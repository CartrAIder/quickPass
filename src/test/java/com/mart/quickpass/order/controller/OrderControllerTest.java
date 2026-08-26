package com.mart.quickpass.order.controller;

import com.mart.quickpass.order.dto.OrderCreateRequest;
import com.mart.quickpass.order.dto.OrderCreateResponse;
import com.mart.quickpass.order.dto.OrderCreateResult;
import com.mart.quickpass.order.dto.OrderDetailResponse;
import com.mart.quickpass.order.dto.OrderHistoryResponse;
import com.mart.quickpass.order.entity.OrderStatus;
import com.mart.quickpass.payment.entity.PaymentStatus;
import com.mart.quickpass.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderControllerTest {

    private final OrderService orderService = mock(OrderService.class);
    private final OrderController controller = new OrderController(orderService);
    private final OrderCreateRequest request = new OrderCreateRequest(List.of());
    private final OrderCreateResponse response = new OrderCreateResponse(
            1L, "order-1", "우유", 3000L, OrderStatus.PENDING_PAYMENT);

    @Test
    void newlyCreatedOrderReturnsCreated() {
        when(orderService.create(1L, request)).thenReturn(OrderCreateResult.created(response));

        var result = controller.create(1L, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    void existingPendingOrderReturnsOk() {
        when(orderService.create(1L, request)).thenReturn(OrderCreateResult.existing(response));

        var result = controller.create(1L, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    void abandonReturnsNoContent() {
        var result = controller.abandon(1L, "order-1");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(orderService).abandon(1L, "order-1");
    }

    @Test
    void purchaseHistoryUsesDocumentedPaginationDefaults() {
        OrderHistoryResponse history = new OrderHistoryResponse(List.of(), 0, 20, false);
        when(orderService.findMyPurchaseHistory(1L, 0, 20)).thenReturn(history);

        var result = controller.findMyPurchaseHistory(1L, 0, 20);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(history);
        verify(orderService).findMyPurchaseHistory(1L, 0, 20);
    }

    @Test
    void purchaseDetailReturnsOrderItemsAndFinalPayment() {
        LocalDateTime purchasedAt = LocalDateTime.of(2026, 8, 22, 14, 30);
        LocalDateTime approvedAt = LocalDateTime.of(2026, 8, 22, 14, 31);
        OrderDetailResponse detail = new OrderDetailResponse(
                "order-1",
                purchasedAt,
                OrderStatus.PAID,
                3000L,
                List.of(new OrderDetailResponse.Item("우유", 3000L, 1, 3000L)),
                new OrderDetailResponse.Payment(PaymentStatus.APPROVED, "카드", 3000L, approvedAt));
        when(orderService.findMyPurchaseDetail(1L, "order-1")).thenReturn(detail);

        var result = controller.findMyPurchaseDetail(1L, "order-1");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(detail);
        verify(orderService).findMyPurchaseDetail(1L, "order-1");
    }
}
