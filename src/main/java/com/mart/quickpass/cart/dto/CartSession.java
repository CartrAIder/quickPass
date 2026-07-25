package com.mart.quickpass.cart.dto;

import com.mart.quickpass.cart.entity.CartStatus;

import java.time.LocalDateTime;

public record CartSession(
        Long userId,
        CartStatus status,
        LocalDateTime startedAt
) {

    public static CartSession start(Long userId) {
        return new CartSession(userId, CartStatus.IN_USE, LocalDateTime.now());
    }
}
