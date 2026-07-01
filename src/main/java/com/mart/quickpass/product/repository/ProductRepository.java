package com.mart.quickpass.product.repository;

import com.mart.quickpass.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
