package com.mart.quickpass.product.service;

import com.mart.quickpass.global.storage.minio.MinioObjectStorage;
import com.mart.quickpass.product.dto.ProductCreateRequest;
import com.mart.quickpass.product.dto.ProductResponse;
import com.mart.quickpass.product.entity.Product;
import com.mart.quickpass.product.entity.ProductCategory;
import com.mart.quickpass.product.entity.ProductStatus;
import com.mart.quickpass.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminProductServiceTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final MinioObjectStorage objectStorage = mock(MinioObjectStorage.class);
    private final AdminProductService adminProductService = new AdminProductService(productRepository, objectStorage);

    @Test
    void createStoresProductWithoutExternalImageUrl() {
        ProductCreateRequest request = new ProductCreateRequest(
                "8800000000001", "서울우유", 3_000, ProductCategory.DAIRY, ProductStatus.ON_SALE
        );
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = adminProductService.create(request);

        assertThat(response.imageUrl()).isNull();
    }
}
