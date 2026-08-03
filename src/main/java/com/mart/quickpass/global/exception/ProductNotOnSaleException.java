package com.mart.quickpass.global.exception;

public class ProductNotOnSaleException extends BusinessException {

    public ProductNotOnSaleException(Long productId) {
        super(ErrorCode.PRODUCT_NOT_ON_SALE, "판매 중인 상품이 아닙니다: " + productId);
    }
}
