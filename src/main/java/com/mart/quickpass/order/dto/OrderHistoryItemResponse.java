package com.mart.quickpass.order.dto;

import com.mart.quickpass.order.entity.Order;
import com.mart.quickpass.order.entity.OrderStatus;

import java.time.LocalDateTime;

public record OrderHistoryItemResponse(
        String orderId,
        String orderName,
        Long totalAmount,
        LocalDateTime purchasedAt,
        OrderStatus status
) {
    public static OrderHistoryItemResponse from(Order order) {
        return new OrderHistoryItemResponse(
                order.getOrderId(),
                order.getOrderName(),
                order.getTotalAmount(),
                order.getPaidAt(),
                order.getStatus()
        );
    }
}
