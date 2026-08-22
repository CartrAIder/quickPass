package com.mart.quickpass.cart.dto;

import com.mart.quickpass.order.entity.Order;

public record PendingOrderResponse(
        String orderId,
        String orderName,
        Long amount
) {
    public static PendingOrderResponse from(Order order) {
        return new PendingOrderResponse(
                order.getOrderId(), order.getOrderName(), order.getTotalAmount());
    }
}
