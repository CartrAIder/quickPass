package com.mart.quickpass.global.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String orderId) {
        super("존재하지 않는 주문입니다: " + orderId);
    }
}
