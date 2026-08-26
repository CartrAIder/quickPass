package com.mart.quickpass.product.dto;

import org.springframework.data.domain.Slice;

import java.util.List;

public record ProductSliceResponse(
        List<ProductResponse> products,
        int page,
        int size,
        boolean hasNext
) {
    public static ProductSliceResponse from(Slice<ProductResponse> products) {
        return new ProductSliceResponse(
                products.getContent(),
                products.getNumber(),
                products.getSize(),
                products.hasNext()
        );
    }
}
