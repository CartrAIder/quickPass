package com.mart.quickpass.global.exception;

public class CartNotFoundException extends RuntimeException {

    public CartNotFoundException(String qrCode) {
        super("존재하지 않는 카트입니다: " + qrCode);
    }
}
