package com.mart.quickpass.order.dto;

import com.mart.quickpass.order.entity.Order;
import com.mart.quickpass.order.entity.OrderItem;
import com.mart.quickpass.order.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record AdminOrderDetailResponse(
        Long id,
        String orderId,
        String orderName,
        Long totalAmount,
        OrderStatus status,
        Customer customer,
        List<Item> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminOrderDetailResponse of(Order order, List<OrderItem> orderItems) {
        return new AdminOrderDetailResponse(
                order.getId(),
                order.getOrderId(),
                order.getOrderName(),
                order.getTotalAmount(),
                order.getStatus(),
                new Customer(order.getUser().getId(), order.getUser().getName(), order.getUser().getEmail()),
                orderItems.stream().map(Item::from).toList(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    public record Customer(Long id, String name, String email) {
    }

    public record Item(
            Long id,
            Long productId,
            String productName,
            Long unitPrice,
            Integer quantity,
            Long lineAmount
    ) {
        private static Item from(OrderItem item) {
            return new Item(
                    item.getId(),
                    item.getProduct().getId(),
                    item.getProductName(),
                    item.getUnitPrice(),
                    item.getQuantity(),
                    item.getLineAmount()
            );
        }
    }
}
