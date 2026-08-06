package com.mart.quickpass.cart.dto;

/** 관리자 카트 운영 목록 및 SSE 이벤트의 단일 카트 상태. */
public record AdminCartResponse(
        Long cartId,
        String qrCode,
        long version,
        AdminCartStatus status,
        AdminCartUserResponse currentUser,
        long productCount,
        long totalAmount
) {
}
