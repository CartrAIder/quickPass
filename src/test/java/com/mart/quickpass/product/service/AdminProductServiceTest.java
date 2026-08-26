package com.mart.quickpass.product.service;

import com.mart.quickpass.product.dto.ProductCreateRequest;
import com.mart.quickpass.product.dto.ProductResponse;
import com.mart.quickpass.product.dto.ProductUpdateRequest;
import com.mart.quickpass.product.entity.Product;
import com.mart.quickpass.product.entity.ProductCategory;
import com.mart.quickpass.product.entity.ProductStatus;
import com.mart.quickpass.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminProductServiceTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final AdminProductService adminProductService = new AdminProductService(productRepository);

    @Test
    void createStoresRepresentativeImageUrl() {
        ProductCreateRequest request = new ProductCreateRequest(
                "8800000000001",
                "서울우유",
                3_000,
                ProductCategory.DAIRY,
                ProductStatus.ON_SALE,
                "https://cdn.example.com/milk.jpg"
        );
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = adminProductService.create(request);

        assertThat(response.imageUrl()).isEqualTo("https://cdn.example.com/milk.jpg");
    }

    @Test
    void updateChangesRepresentativeImageUrl() {
        Product product = Product.builder()
                .barcode("8800000000001")
                .name("서울우유")
                .price(3_000)
                .category(ProductCategory.DAIRY)
                .status(ProductStatus.ON_SALE)
                .imageUrl("https://cdn.example.com/old.jpg")
                .build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        ProductUpdateRequest request = new ProductUpdateRequest(
                null,
                null,
                null,
                "https://cdn.example.com/new.jpg"
        );

        ProductResponse response = adminProductService.update(1L, request);

        assertThat(response.imageUrl()).isEqualTo("https://cdn.example.com/new.jpg");
    }
}
