package com.mart.quickpass.cart.event;

// 장바구니 상태 변경 이벤트
public record CartChangedEvent(
        Long userId, // 사용자
        String qrCode, // qr 코드
        CartChangeType type, // 변경의 종류
        long version // 버전
) {

    public static CartChangedEvent of(Long userId, String qrCode, CartChangeType type, long version) {
        return new CartChangedEvent(userId, qrCode, type, version);
    }
}
