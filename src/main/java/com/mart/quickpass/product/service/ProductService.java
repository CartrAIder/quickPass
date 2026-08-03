package com.mart.quickpass.product.service;

import com.mart.quickpass.global.exception.ProductNotFoundException;
import com.mart.quickpass.product.dto.ProductResponse;
import com.mart.quickpass.product.dto.ProductSortType;
import com.mart.quickpass.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    // 상품 전체 조회
    public List<ProductResponse> findAll(ProductSortType sortType) {
        return productRepository.findAll(toSort(sortType)).stream()
                .map(ProductResponse::from)
                .toList();
    }

    // 조회 순서
    private Sort toSort(ProductSortType sortType) {
        Sort sort = switch (sortType) {
            case LATEST -> Sort.by(Sort.Direction.DESC, "id");
            case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "price");
            case PRICE_DESC -> Sort.by(Sort.Direction.DESC, "price");
            case NAME_ASC -> Sort.by(Sort.Direction.ASC, "name");
        };

        return sortType == ProductSortType.LATEST
                ? sort
                : sort.and(Sort.by(Sort.Direction.DESC, "id"));
    }

    // 상품 상세 조회
    public ProductResponse findById(Long productId) {
        return productRepository.findById(productId)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }
}
