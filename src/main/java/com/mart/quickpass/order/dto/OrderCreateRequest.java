package com.mart.quickpass.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OrderCreateRequest(
        // 상품 리스트 dto

        @NotEmpty(message = "주문 상품은 최소 1개 이상이어야 합니다.")
        List<@Valid OrderCreateItemRequest> items
) {
}
