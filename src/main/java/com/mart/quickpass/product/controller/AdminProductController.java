package com.mart.quickpass.product.controller;

import com.mart.quickpass.product.dto.ProductCreateRequest;
import com.mart.quickpass.product.dto.ProductImageUploadResponse;
import com.mart.quickpass.product.dto.ProductResponse;
import com.mart.quickpass.product.dto.ProductUpdateRequest;
import com.mart.quickpass.product.service.AdminProductService;
import com.mart.quickpass.product.service.ProductImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService adminProductService;
    private final ProductImageService productImageService;

    @PostMapping("/{barcode}/image")
    public ResponseEntity<ProductImageUploadResponse> uploadImage(
            @PathVariable String barcode,
            @RequestParam(defaultValue = "false") boolean replace,
            @RequestPart("image") MultipartFile image
    ) {
        ProductImageUploadResponse response = productImageService.upload(barcode, image, replace);
        return ResponseEntity.status(response.uploaded() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(response);
    }

    // 상품 등록
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminProductService.create(request));
    }

    // 상품 정보 수정
    @PatchMapping("/{productId}")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request) {
        return ResponseEntity.ok(adminProductService.update(productId, request));
    }
}
