package com.mart.quickpass.product.repository;

import com.mart.quickpass.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByBarcode(String barcode);

    Optional<Product> findByBarcode(String barcode);
}
