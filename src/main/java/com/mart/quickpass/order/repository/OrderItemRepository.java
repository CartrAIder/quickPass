package com.mart.quickpass.order.repository;

import com.mart.quickpass.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
