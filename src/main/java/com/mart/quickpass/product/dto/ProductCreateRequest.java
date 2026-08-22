package com.mart.quickpass.product.dto;

import com.mart.quickpass.product.entity.ProductCategory;
import com.mart.quickpass.product.entity.ProductStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProductCreateRequest(
        // 상품 등록 dto

        @NotBlank(message = "바코드는 필수입니다.")
        @Size(max = 100, message = "바코드는 100자를 초과할 수 없습니다.")
        String barcode,

        @NotBlank(message = "상품명은 필수입니다.")
        @Size(max = 255, message = "상품명은 255자를 초과할 수 없습니다.")
        String name,

        @NotNull(message = "가격은 필수입니다.")
        @Positive(message = "가격은 0보다 커야 합니다.")
        Integer price,

        @NotNull(message = "카테고리는 필수입니다.")
        ProductCategory category,

        @NotNull(message = "판매 상태는 필수입니다.")
        ProductStatus status,

        @Size(max = 2048, message = "이미지 URL은 2048자를 초과할 수 없습니다.")
        String imageUrl
) {
}
