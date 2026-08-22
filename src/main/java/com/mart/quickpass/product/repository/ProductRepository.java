package com.mart.quickpass.product.repository;

import com.mart.quickpass.product.entity.Product;
import com.mart.quickpass.product.entity.ProductCategory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByBarcode(String barcode);

    boolean existsByBarcodeAndIdNot(String barcode, Long id);

    Optional<Product> findByBarcode(String barcode);

    @Query("""
            SELECT product
            FROM Product product
            WHERE (:keyword IS NULL OR LOWER(product.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:category IS NULL OR product.category = :category)
            """)
    Slice<Product> search(
            @Param("keyword") String keyword,
            @Param("category") ProductCategory category,
            Pageable pageable
    );
}
