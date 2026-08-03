package com.mart.quickpass.product.service;

import com.mart.quickpass.product.dto.ProductSortType;
import com.mart.quickpass.product.repository.ProductRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductService productService = new ProductService(productRepository);

    @ParameterizedTest
    @MethodSource("sortCases")
    void findAllAppliesRequestedSort(ProductSortType sortType, Sort expectedSort) {
        when(productRepository.findAll(expectedSort)).thenReturn(List.of());

        productService.findAll(sortType);

        verify(productRepository).findAll(expectedSort);
    }

    private static Stream<Arguments> sortCases() {
        Sort latest = Sort.by(Sort.Direction.DESC, "id");
        Sort tieBreaker = Sort.by(Sort.Direction.DESC, "id");

        return Stream.of(
                Arguments.of(ProductSortType.LATEST, latest),
                Arguments.of(ProductSortType.PRICE_ASC,
                        Sort.by(Sort.Direction.ASC, "price").and(tieBreaker)),
                Arguments.of(ProductSortType.PRICE_DESC,
                        Sort.by(Sort.Direction.DESC, "price").and(tieBreaker)),
                Arguments.of(ProductSortType.NAME_ASC,
                        Sort.by(Sort.Direction.ASC, "name").and(tieBreaker))
        );
    }
}
