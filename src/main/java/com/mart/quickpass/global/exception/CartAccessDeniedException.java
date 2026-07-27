package com.mart.quickpass.global.exception;

public class CartAccessDeniedException extends BusinessException {

    public CartAccessDeniedException() {
        super(ErrorCode.CART_ACCESS_DENIED, "본인이 연결한 카트만 조작할 수 있습니다.");
    }
}
