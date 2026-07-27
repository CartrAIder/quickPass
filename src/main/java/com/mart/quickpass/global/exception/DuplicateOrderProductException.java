package com.mart.quickpass.global.exception;

public class DuplicateOrderProductException extends RuntimeException {

    public DuplicateOrderProductException(Long productId) {
        super("주문 상품이 중복되었습니다: " + productId);
    }
}
