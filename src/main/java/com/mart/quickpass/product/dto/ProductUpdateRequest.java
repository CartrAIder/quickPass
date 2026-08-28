package com.mart.quickpass.product.dto;

import com.mart.quickpass.product.entity.ProductStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProductUpdateRequest(
        // 상품 갱신 dto

        @Pattern(regexp = "[A-Za-z0-9._-]+", message = "바코드는 영문, 숫자, 점, 밑줄, 하이픈만 사용할 수 있습니다.")
        @Size(max = 100, message = "바코드는 100자를 초과할 수 없습니다.")
        String barcode,

        @Positive(message = "가격은 0보다 커야 합니다.")
        Integer price,

        ProductStatus status
) {

    @AssertTrue(message = "수정할 상품 정보를 하나 이상 입력해야 합니다.")
    public boolean isUpdateRequested() {
        return barcode != null || price != null || status != null;
    }
}
