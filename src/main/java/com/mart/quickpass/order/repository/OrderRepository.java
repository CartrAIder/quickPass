package com.mart.quickpass.order.repository;

import com.mart.quickpass.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
