package com.mart.quickpass.product.service;

import com.mart.quickpass.global.exception.ProductNotFoundException;
import com.mart.quickpass.global.storage.minio.MinioObjectStorage;
import com.mart.quickpass.product.dto.ProductResponse;
import com.mart.quickpass.product.dto.ProductSliceResponse;
import com.mart.quickpass.product.dto.ProductSortType;
import com.mart.quickpass.product.entity.ProductCategory;
import com.mart.quickpass.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final MinioObjectStorage objectStorage;

    public ProductSliceResponse search(
            String keyword,
            ProductCategory category,
            ProductSortType sortType,
            int page,
            int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, toSort(sortType));
        return ProductSliceResponse.from(productRepository.search(normalizeKeyword(keyword), category, pageable)
                .map(this::toResponse));
    }

    // 조회 순서
    private Sort toSort(ProductSortType sortType) {
        Sort selectedSort = switch (sortType) {
            case NAME_ASC -> Sort.by(Sort.Direction.ASC, "name");
            case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "price");
            case PRICE_DESC -> Sort.by(Sort.Direction.DESC, "price");
        };

        return Sort.by(Sort.Direction.ASC, "status")
                .and(selectedSort)
                .and(Sort.by(Sort.Direction.ASC, "id"));
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    // 상품 상세 조회
    public ProductResponse findById(Long productId) {
        return productRepository.findById(productId)
                .map(this::toResponse)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private ProductResponse toResponse(com.mart.quickpass.product.entity.Product product) {
        return ProductResponse.from(product, objectStorage::publicUrl);
    }
}
