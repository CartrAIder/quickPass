package com.mart.quickpass.product.dto;

import com.mart.quickpass.product.entity.Product;
import com.mart.quickpass.product.entity.ProductStatus;

public record ProductResponse(
        // 응답 dto

        Long id,
        String barcode,
        String name,
        Integer price,
        String category,
        ProductStatus status
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getBarcode(),
                product.getName(),
                product.getPrice(),
                product.getCategory(),
                product.getStatus()
        );
    }
}
