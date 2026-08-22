package com.mart.quickpass.global.exception;

public class CartPaymentPendingException extends BusinessException {

    public CartPaymentPendingException() {
        super(ErrorCode.CART_PAYMENT_PENDING, "결제 진행 중에는 장바구니를 변경할 수 없습니다.");
    }
}
