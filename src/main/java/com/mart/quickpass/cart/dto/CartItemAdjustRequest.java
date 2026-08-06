package com.mart.quickpass.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CartItemAdjustRequest(
        // 상품 개수 dto

        @NotNull(message = "상품 수량(delta)은 필수입니다.")
        @PositiveOrZero(message = "상품 수량(delta)은 0 이상이어야 합니다.")
        Long delta
) {
}
