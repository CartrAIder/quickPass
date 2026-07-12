package com.mart.quickpass.global.exception;

public class CartSessionNotFoundException extends RuntimeException {

    public CartSessionNotFoundException(String qrCode) {
        super("연동되지 않은 카트입니다: " + qrCode);
    }
}
