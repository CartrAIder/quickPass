package com.mart.quickpass.global.exception;

public class ProductNotFoundException extends BusinessException {

    public ProductNotFoundException(Long productId) {
        super(ErrorCode.PRODUCT_NOT_FOUND, "존재하지 않는 상품입니다: " + productId);
    }

    public ProductNotFoundException(String barcode) {
        super(ErrorCode.PRODUCT_NOT_FOUND, "존재하지 않는 상품입니다: " + barcode);
    }
}
