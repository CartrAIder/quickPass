package com.mart.quickpass.product.dto;

import com.mart.quickpass.product.entity.ProductCategory;

public record ProductCategoryResponse(
        String code,
        String name
) {
    public static ProductCategoryResponse from(ProductCategory category) {
        return new ProductCategoryResponse(category.name(), category.getDisplayName());
    }
}
