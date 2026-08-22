package com.mart.quickpass.global.exception;

public class ProductImageUploadException extends BusinessException {

    public ProductImageUploadException(Throwable cause) {
        super(ErrorCode.PRODUCT_IMAGE_UPLOAD_FAILED, "상품 이미지 저장에 실패했습니다.");
        initCause(cause);
    }
}
