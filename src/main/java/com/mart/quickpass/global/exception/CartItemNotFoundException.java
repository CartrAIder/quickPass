package com.mart.quickpass.global.exception;

public class CartItemNotFoundException extends RuntimeException {

    public CartItemNotFoundException(String barcode) {
        super("장바구니에 없는 상품입니다: " + barcode);
    }
}
