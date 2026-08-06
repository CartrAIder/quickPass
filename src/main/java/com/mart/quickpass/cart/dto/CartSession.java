package com.mart.quickpass.cart.dto;

import java.time.LocalDateTime;

public record CartSession(
        Long userId,
        LocalDateTime startedAt
) {

    public static CartSession start(Long userId) {
        return new CartSession(userId, LocalDateTime.now());
    }
}
