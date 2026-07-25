package com.mart.quickpass.cart.dto;

import jakarta.validation.constraints.NotNull;

public record CartItemAdjustRequest(
        // 상품 개수 dto

        @NotNull(message = "증감 수량(delta)은 필수입니다.")
        Long delta
) {
}
