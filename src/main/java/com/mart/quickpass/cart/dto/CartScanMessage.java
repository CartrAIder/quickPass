package com.mart.quickpass.cart.dto;

/**
 * ESP32 카트가 발행하는 바코드 스캔 메시지 payload.
 *
 * <p>예시 JSON: {@code {"barcode": "8801234567890", "scannedAt": 1720000000000}}
 *
 * @param barcode   스캔된 상품 바코드
 * @param scannedAt 카트에서 스캔한 시각(epoch millis). 없으면 null 허용.
 */
public record CartScanMessage(
        String barcode,
        Long scannedAt
) {
}
