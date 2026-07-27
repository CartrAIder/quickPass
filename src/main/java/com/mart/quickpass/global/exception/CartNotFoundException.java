package com.mart.quickpass.global.exception;

public class CartNotFoundException extends BusinessException {

    public CartNotFoundException(String qrCode) {
        super(ErrorCode.CART_NOT_FOUND, "존재하지 않는 카트입니다: " + qrCode);
    }
}
