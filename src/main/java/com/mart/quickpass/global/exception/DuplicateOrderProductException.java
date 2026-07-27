package com.mart.quickpass.global.exception;

public class DuplicateOrderProductException extends BusinessException {

    public DuplicateOrderProductException(Long productId) {
        super(ErrorCode.DUPLICATE_ORDER_PRODUCT, "주문 상품이 중복되었습니다: " + productId);
    }
}
