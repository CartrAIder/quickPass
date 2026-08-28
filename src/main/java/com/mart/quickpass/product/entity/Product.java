package com.mart.quickpass.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 기본키

    @Column(nullable = false, unique = true)
    private String barcode; // 바코드

    @Column(nullable = false)
    private String name; // 상품명

    @Column(nullable = false)
    private Integer price; // 상품 가격

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductCategory category; // 카테고리

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status; // 상태

    @Column(length = 512)
    private String imageKey; // 저장소 주소와 독립적인 MinIO Object Key

    @Builder
    public Product(
            String barcode,
            String name,
            Integer price,
            ProductCategory category,
            ProductStatus status,
            String imageKey
    ) {
        this.barcode = barcode;
        this.name = name;
        this.price = price;
        this.category = category;
        this.status = status;
        this.imageKey = imageKey;
    }

    public void changePrice(Integer price) {
        this.price = price;
    }

    public void changeStatus(ProductStatus status) {
        this.status = status;
    }

    public void changeBarcode(String barcode) {
        this.barcode = barcode;
    }

    public void changeImageKey(String imageKey) {
        this.imageKey = imageKey;
    }
}
