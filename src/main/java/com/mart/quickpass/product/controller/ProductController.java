package com.mart.quickpass.product.controller;

import com.mart.quickpass.product.dto.ProductCategoryResponse;
import com.mart.quickpass.product.dto.ProductResponse;
import com.mart.quickpass.product.dto.ProductSliceResponse;
import com.mart.quickpass.product.dto.ProductSortType;
import com.mart.quickpass.product.entity.ProductCategory;
import com.mart.quickpass.product.service.ProductService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ProductSliceResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(defaultValue = "NAME_ASC") ProductSortType sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(productService.search(keyword, category, sort, page, size));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<ProductCategoryResponse>> findCategories() {
        return ResponseEntity.ok(Arrays.stream(ProductCategory.values())
                .map(ProductCategoryResponse::from)
                .toList());
    }

    // 개별 상품 조회 컨트롤러
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> findById(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.findById(productId));
    }
}
