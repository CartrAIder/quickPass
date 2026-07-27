package com.mart.quickpass.global.exception;

public class OrderNotFoundException extends BusinessException {

    public OrderNotFoundException(String orderId) {
        super(ErrorCode.ORDER_NOT_FOUND, "존재하지 않는 주문입니다: " + orderId);
    }
}
