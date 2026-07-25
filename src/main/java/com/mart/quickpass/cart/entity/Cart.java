package com.mart.quickpass.cart.entity;

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
@Table(name = "carts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 기본키

    @Column(name = "qr_code", nullable = false, unique = true)
    private String qrCode; // qrcode

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CartStatus status; // 카트 상태(대기중, 사용중)

    @Builder
    public Cart(String qrCode, CartStatus status) {
        this.qrCode = qrCode;
        this.status = status;
    }

    public void markInUse() {
        this.status = CartStatus.IN_USE;
    }

    public void markWaiting() {
        this.status = CartStatus.WAITING;
    }
}
