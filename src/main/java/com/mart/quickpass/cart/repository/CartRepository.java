package com.mart.quickpass.cart.repository;

import com.mart.quickpass.cart.entity.Cart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    boolean existsByQrCode(String qrCode);

    Optional<Cart> findByQrCode(String qrCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cart c where c.qrCode = :qrCode")
    Optional<Cart> findByQrCodeForUpdate(@Param("qrCode") String qrCode);
}
