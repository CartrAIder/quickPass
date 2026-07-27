package com.mart.quickpass.global.exception;

public class CartAlreadyInUseException extends BusinessException {

    public CartAlreadyInUseException(String qrCode) {
        super(ErrorCode.CART_ALREADY_IN_USE, "이미 사용 중인 카트입니다: " + qrCode);
    }
}
