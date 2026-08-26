package com.mart.quickpass.order.dto;

import org.springframework.data.domain.Slice;

import java.util.List;

public record OrderHistoryResponse(
        List<OrderHistoryItemResponse> orders,
        int page,
        int size,
        boolean hasNext
) {
    public static OrderHistoryResponse from(Slice<OrderHistoryItemResponse> orders) {
        return new OrderHistoryResponse(
                orders.getContent(),
                orders.getNumber(),
                orders.getSize(),
                orders.hasNext()
        );
    }
}
