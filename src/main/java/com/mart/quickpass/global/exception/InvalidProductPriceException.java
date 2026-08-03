package com.mart.quickpass.global.exception;

public class InvalidProductPriceException extends BusinessException {

    public InvalidProductPriceException(Long productId) {
        super(ErrorCode.INVALID_PRODUCT_PRICE, "상품 가격은 0원보다 커야 합니다: " + productId);
    }
}
