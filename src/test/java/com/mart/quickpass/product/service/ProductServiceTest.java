package com.mart.quickpass.product.service;

import com.mart.quickpass.global.storage.minio.MinioObjectStorage;
import com.mart.quickpass.product.dto.ProductSliceResponse;
import com.mart.quickpass.product.dto.ProductSortType;
import com.mart.quickpass.product.entity.Product;
import com.mart.quickpass.product.entity.ProductCategory;
import com.mart.quickpass.product.entity.ProductStatus;
import com.mart.quickpass.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final MinioObjectStorage objectStorage = mock(MinioObjectStorage.class);
    private final ProductService productService = new ProductService(productRepository, objectStorage);

    @ParameterizedTest
    @MethodSource("sortCases")
    void searchAlwaysSortsOnSaleProductsBeforeSoldOutProducts(
            ProductSortType sortType,
            Sort expectedSort
    ) {
        PageRequest pageable = PageRequest.of(2, 10, expectedSort);
        when(productRepository.search("우유", ProductCategory.DAIRY, pageable))
                .thenReturn(new SliceImpl<>(List.of(), pageable, false));

        productService.search(" 우유 ", ProductCategory.DAIRY, sortType, 2, 10);

        verify(productRepository).search("우유", ProductCategory.DAIRY, pageable);
    }

    @Test
    void searchReturnsProductsAndSliceMetadata() {
        Product product = Product.builder()
                .barcode("8800000000001")
                .name("서울우유")
                .price(3_000)
                .category(ProductCategory.DAIRY)
                .status(ProductStatus.ON_SALE)
                .imageKey("products/8800000000001/milk.jpg")
                .build();
        PageRequest pageable = PageRequest.of(0, 1,
                Sort.by("status").ascending()
                        .and(Sort.by("name").ascending())
                        .and(Sort.by("id").ascending()));
        when(productRepository.search(null, null, pageable))
                .thenReturn(new SliceImpl<>(List.of(product), pageable, true));
        when(objectStorage.publicUrl("products/8800000000001/milk.jpg"))
                .thenReturn("https://cdn.example.com/product-images/products/8800000000001/milk.jpg");

        ProductSliceResponse response = productService.search("   ", null, ProductSortType.NAME_ASC, 0, 1);

        assertThat(response.products()).singleElement().satisfies(item -> {
            assertThat(item.name()).isEqualTo("서울우유");
            assertThat(item.category()).isEqualTo(ProductCategory.DAIRY);
            assertThat(item.status()).isEqualTo(ProductStatus.ON_SALE);
            assertThat(item.imageUrl()).isEqualTo(
                    "https://cdn.example.com/product-images/products/8800000000001/milk.jpg");
        });
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(1);
        assertThat(response.hasNext()).isTrue();
    }

    private static Stream<Arguments> sortCases() {
        Sort statusFirst = Sort.by(Sort.Direction.ASC, "status");
        Sort idTieBreaker = Sort.by(Sort.Direction.ASC, "id");

        return Stream.of(
                Arguments.of(ProductSortType.NAME_ASC,
                        statusFirst.and(Sort.by(Sort.Direction.ASC, "name")).and(idTieBreaker)),
                Arguments.of(ProductSortType.PRICE_ASC,
                        statusFirst.and(Sort.by(Sort.Direction.ASC, "price")).and(idTieBreaker)),
                Arguments.of(ProductSortType.PRICE_DESC,
                        statusFirst.and(Sort.by(Sort.Direction.DESC, "price")).and(idTieBreaker))
        );
    }
}
