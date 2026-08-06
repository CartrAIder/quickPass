package com.mart.quickpass.global.exception;

public class InvalidCartItemQuantityException extends BusinessException {

    public InvalidCartItemQuantityException(long quantity) {
        super(ErrorCode.INVALID_CART_ITEM_QUANTITY, "상품 수량은 0 이상이어야 합니다: " + quantity);
    }
}
