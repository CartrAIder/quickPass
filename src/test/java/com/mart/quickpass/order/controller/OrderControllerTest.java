package com.mart.quickpass.order.controller;

import com.mart.quickpass.order.dto.OrderCreateRequest;
import com.mart.quickpass.order.dto.OrderCreateResponse;
import com.mart.quickpass.order.dto.OrderCreateResult;
import com.mart.quickpass.order.entity.OrderStatus;
import com.mart.quickpass.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

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
}
