package com.mart.quickpass.order.repository;

import com.mart.quickpass.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @EntityGraph(attributePaths = "product")
    // 조회 시 product도 함께 가져온다
    List<OrderItem> findAllByOrderIdOrderByIdAsc(Long orderId);
}
