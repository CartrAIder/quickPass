package com.mart.quickpass.order.dto;

import com.mart.quickpass.order.entity.Order;
import com.mart.quickpass.order.entity.OrderItem;
import com.mart.quickpass.order.entity.OrderStatus;
import com.mart.quickpass.payment.entity.PaymentAttempt;
import com.mart.quickpass.payment.entity.PaymentStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponse(
        String orderId,
        LocalDateTime purchasedAt,
        OrderStatus status,
        Long totalAmount,
        List<Item> items,
        Payment payment
) {
    public static OrderDetailResponse of(
            Order order,
            List<OrderItem> orderItems,
            PaymentAttempt paymentAttempt
    ) {
        return new OrderDetailResponse(
                order.getOrderId(),
                order.getPaidAt(),
                order.getStatus(),
                order.getTotalAmount(),
                orderItems.stream().map(Item::from).toList(),
                Payment.from(paymentAttempt)
        );
    }

    public record Item(String productName, Long unitPrice, Integer quantity, Long lineAmount) {
        private static Item from(OrderItem orderItem) {
            return new Item(
                    orderItem.getProductName(),
                    orderItem.getUnitPrice(),
                    orderItem.getQuantity(),
                    orderItem.getLineAmount()
            );
        }
    }

    public record Payment(PaymentStatus status, String method, Long amount, LocalDateTime approvedAt) {
        private static Payment from(PaymentAttempt paymentAttempt) {
            return new Payment(
                    paymentAttempt.getStatus(),
                    paymentAttempt.getMethod(),
                    paymentAttempt.getApprovedAmount(),
                    paymentAttempt.getApprovedAt()
            );
        }
    }
}
