package com.mart.quickpass.order.dto;

import com.mart.quickpass.order.entity.Order;
import com.mart.quickpass.order.entity.OrderStatus;

public record OrderCreateResponse(
        Long id,
        String orderId,
        String orderName,
        Long totalAmount,
        OrderStatus status
) {

    public static OrderCreateResponse from(Order order) {
        return new OrderCreateResponse(
                order.getId(),
                order.getOrderId(),
                order.getOrderName(),
                order.getTotalAmount(),
                order.getStatus()
        );
    }
}
