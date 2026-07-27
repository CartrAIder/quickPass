package com.mart.quickpass.global.exception;

public class CartSessionNotFoundException extends BusinessException {

    public CartSessionNotFoundException(String qrCode) {
        super(ErrorCode.CART_SESSION_NOT_FOUND, "연동되지 않은 카트입니다: " + qrCode);
    }
}
