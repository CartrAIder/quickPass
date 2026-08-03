package com.mart.quickpass.product.controller;

import com.mart.quickpass.product.dto.ProductResponse;
import com.mart.quickpass.product.dto.ProductSortType;
import com.mart.quickpass.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // 전체 상품 보기 컨트롤러
    @GetMapping
    public ResponseEntity<List<ProductResponse>> findAll(
            @RequestParam(defaultValue = "LATEST") ProductSortType sort
    ) {
        return ResponseEntity.ok(productService.findAll(sort));
    }

    // 개별 상품 조회 컨트롤러
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> findById(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.findById(productId));
    }
}
