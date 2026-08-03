package com.mart.quickpass.order.entity;

public enum OrderStatus {
    PENDING_PAYMENT, // 결제 대기
    PAID,            // 결제 완료
    CANCELED,        // 주문 취소
    EXPIRED          // 주문 만료
}
