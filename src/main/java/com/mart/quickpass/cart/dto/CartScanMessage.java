package com.mart.quickpass.cart.dto;


public record CartScanMessage(
        String scanId, // 실제 스캔 1회를 식별하는 고유 ID. QoS 1 재전송에도 동일 값을 사용한다.
        String barcode, // 바코드 정보
        Long scannedAt // 스캔 시간
) {
}
