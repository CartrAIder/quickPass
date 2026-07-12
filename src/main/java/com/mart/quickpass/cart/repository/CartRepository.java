package com.mart.quickpass.cart.repository;

import com.mart.quickpass.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    boolean existsByQrCode(String qrCode);

    Optional<Cart> findByQrCode(String qrCode);
}
