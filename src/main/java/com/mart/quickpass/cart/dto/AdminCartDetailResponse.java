package com.mart.quickpass.cart.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 관리자가 특정 카트의 연결 및 장바구니 상태를 확인할 때 사용하는 응답. */
public record AdminCartDetailResponse(
        Long cartId,
        String qrCode,
        long version,
        AdminCartStatus status,
        AdminCartUserResponse currentUser,
        LocalDateTime connectedAt,
        List<CartItemResponse> items,
        long totalQuantity,
        long totalAmount
) {
}
