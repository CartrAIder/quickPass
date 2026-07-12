package com.mart.quickpass.global.exception;

public class CartAccessDeniedException extends RuntimeException {

    public CartAccessDeniedException() {
        super("본인이 연결한 카트만 조작할 수 있습니다.");
    }
}
