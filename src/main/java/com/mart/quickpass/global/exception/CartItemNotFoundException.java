package com.mart.quickpass.global.exception;

public class CartItemNotFoundException extends BusinessException {

    public CartItemNotFoundException(String barcode) {
        super(ErrorCode.CART_ITEM_NOT_FOUND, "장바구니에 없는 상품입니다: " + barcode);
    }
}
