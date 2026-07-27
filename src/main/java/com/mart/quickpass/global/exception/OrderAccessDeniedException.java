package com.mart.quickpass.global.exception;

public class OrderAccessDeniedException extends RuntimeException {

    public OrderAccessDeniedException() {
        super("본인의 주문만 결제할 수 있습니다.");
    }
}
