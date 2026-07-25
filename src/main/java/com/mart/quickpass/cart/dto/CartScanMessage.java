package com.mart.quickpass.cart.dto;


public record CartScanMessage(
        String barcode, // 바코드 정보
        Long scannedAt // 스캔 시간
) {
}
