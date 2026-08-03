package com.mart.quickpass.product.service;

import com.mart.quickpass.global.exception.DuplicateProductBarcodeException;
import com.mart.quickpass.global.exception.ProductNotFoundException;
import com.mart.quickpass.product.dto.ProductCreateRequest;
import com.mart.quickpass.product.dto.ProductResponse;
import com.mart.quickpass.product.dto.ProductUpdateRequest;
import com.mart.quickpass.product.entity.Product;
import com.mart.quickpass.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProductService {

    private final ProductRepository productRepository;

    // 상품 등록 메서드
    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
        validateUniqueBarcode(request.barcode(), null);
        Product product = Product.builder()
                .barcode(request.barcode())
                .name(request.name())
                .price(request.price())
                .category(request.category())
                .status(request.status())
                .build();
        return ProductResponse.from(productRepository.save(product));
    }

    // 상품 정보 수정 메서드
    @Transactional
    public ProductResponse update(Long productId, ProductUpdateRequest request) {
        Product product = getProduct(productId);

        if (request.barcode() != null) {
            validateUniqueBarcode(request.barcode(), productId);
            product.changeBarcode(request.barcode());
        }
        if (request.price() != null) {
            product.changePrice(request.price());
        }
        if (request.status() != null) {
            product.changeStatus(request.status());
        }

        return ProductResponse.from(product);
    }

    // 상품 정보 조회 메서드
    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    // 바코드 중복 검사 메서드
    private void validateUniqueBarcode(String barcode, Long productId) {
        boolean duplicate = productId == null
                ? productRepository.existsByBarcode(barcode)
                : productRepository.existsByBarcodeAndIdNot(barcode, productId);
        if (duplicate) {
            throw new DuplicateProductBarcodeException(barcode);
        }
    }
}
