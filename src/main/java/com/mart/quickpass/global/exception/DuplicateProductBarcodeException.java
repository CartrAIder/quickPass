package com.mart.quickpass.global.exception;

public class DuplicateProductBarcodeException extends BusinessException {

    public DuplicateProductBarcodeException(String barcode) {
        super(ErrorCode.DUPLICATE_PRODUCT_BARCODE, "이미 등록된 바코드입니다: " + barcode);
    }
}
