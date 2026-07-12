package com.mart.quickpass.cart.dto;

import com.mart.quickpass.cart.entity.CartStatus;

import java.time.LocalDateTime;

/**
 * Redis에 저장되는 "카트 점유 세션" (Key: {@code cart:session:{qrCode}}).
 *
 * <p>앱이 QR을 스캔해 '물리적 카트'와 '논리적 사용자'를 매핑한 결과이며,
 * MQTT로 들어오는 바코드 스캔 이벤트가 어느 사용자의 장바구니에 반영되어야 하는지 식별하는 기준이 된다.
 */
public record CartSession(
        Long userId,
        CartStatus status,
        LocalDateTime startedAt
) {

    public static CartSession start(Long userId) {
        return new CartSession(userId, CartStatus.IN_USE, LocalDateTime.now());
    }
}
