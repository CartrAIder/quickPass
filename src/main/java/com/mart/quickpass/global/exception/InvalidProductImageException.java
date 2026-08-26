package com.mart.quickpass.global.exception;

public class InvalidProductImageException extends BusinessException {

    public InvalidProductImageException(String message) {
        super(ErrorCode.INVALID_PRODUCT_IMAGE, message);
    }
}
