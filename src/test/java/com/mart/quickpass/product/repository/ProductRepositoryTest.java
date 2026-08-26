package com.mart.quickpass.product.repository;

import com.mart.quickpass.product.entity.Product;
import com.mart.quickpass.product.entity.ProductCategory;
import com.mart.quickpass.product.entity.ProductStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void searchFiltersByProductNameAndCategoryAndKeepsOnSaleProductsFirst() {
        productRepository.saveAll(List.of(
                product("9900000000001", "통합검색우유 B", ProductCategory.DAIRY, ProductStatus.ON_SALE),
                product("9900000000002", "통합검색우유 A", ProductCategory.DAIRY, ProductStatus.ON_SALE),
                product("9900000000003", "통합검색우유 C", ProductCategory.DAIRY, ProductStatus.SOLD_OUT),
                product("9900000000004", "통합검색우유 음료", ProductCategory.BEVERAGE, ProductStatus.ON_SALE)
        ));
        Sort sort = Sort.by("status").ascending()
                .and(Sort.by("name").ascending())
                .and(Sort.by("id").ascending());

        Slice<Product> firstPage = productRepository.search(
                "검색우유",
                ProductCategory.DAIRY,
                PageRequest.of(0, 2, sort)
        );
        Slice<Product> secondPage = productRepository.search(
                "검색우유",
                ProductCategory.DAIRY,
                PageRequest.of(1, 2, sort)
        );

        assertThat(firstPage.getContent())
                .extracting(Product::getName)
                .containsExactly("통합검색우유 A", "통합검색우유 B");
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.getContent())
                .extracting(Product::getName)
                .containsExactly("통합검색우유 C");
        assertThat(secondPage.hasNext()).isFalse();
    }

    private Product product(
            String barcode,
            String name,
            ProductCategory category,
            ProductStatus status
    ) {
        return Product.builder()
                .barcode(barcode)
                .name(name)
                .price(1_000)
                .category(category)
                .status(status)
                .build();
    }
}
