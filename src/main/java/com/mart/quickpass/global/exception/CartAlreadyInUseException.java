package com.mart.quickpass.global.exception;

public class CartAlreadyInUseException extends RuntimeException {

    public CartAlreadyInUseException(String qrCode) {
        super("이미 사용 중인 카트입니다: " + qrCode);
    }
}
