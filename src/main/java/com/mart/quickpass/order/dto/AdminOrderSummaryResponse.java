package com.mart.quickpass.order.dto;

import com.mart.quickpass.order.entity.Order;
import com.mart.quickpass.order.entity.OrderStatus;

import java.time.LocalDateTime;

public record AdminOrderSummaryResponse(
        Long id,
        String orderId,
        String orderName,
        Long totalAmount,
        OrderStatus status,
        Long userId,
        String userName,
        String userEmail,
        LocalDateTime createdAt
) {
    public static AdminOrderSummaryResponse from(Order order) {
        return new AdminOrderSummaryResponse(
                order.getId(),
                order.getOrderId(),
                order.getOrderName(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getUser().getId(),
                order.getUser().getName(),
                order.getUser().getEmail(),
                order.getCreatedAt()
        );
    }
}
