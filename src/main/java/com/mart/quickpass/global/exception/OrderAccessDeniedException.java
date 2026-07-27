package com.mart.quickpass.global.exception;

public class OrderAccessDeniedException extends BusinessException {

    public OrderAccessDeniedException() {
        super(ErrorCode.ORDER_ACCESS_DENIED, "본인의 주문만 결제할 수 있습니다.");
    }
}
