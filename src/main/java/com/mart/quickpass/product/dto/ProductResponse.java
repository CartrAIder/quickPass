package com.mart.quickpass.product.dto;

import com.mart.quickpass.product.entity.Product;
import com.mart.quickpass.product.entity.ProductCategory;
import com.mart.quickpass.product.entity.ProductStatus;

import java.util.function.Function;

public record ProductResponse(
        // 응답 dto

        Long id,
        String barcode,
        String name,
        Integer price,
        ProductCategory category,
        ProductStatus status,
        String imageUrl
) {
    public static ProductResponse from(Product product, Function<String, String> imageUrlResolver) {
        return new ProductResponse(
                product.getId(),
                product.getBarcode(),
                product.getName(),
                product.getPrice(),
                product.getCategory(),
                product.getStatus(),
                imageUrlResolver.apply(product.getImageKey())
        );
    }
}
