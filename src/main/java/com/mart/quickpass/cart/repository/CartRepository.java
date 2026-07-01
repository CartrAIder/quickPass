package com.mart.quickpass.cart.repository;

import com.mart.quickpass.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {
}
