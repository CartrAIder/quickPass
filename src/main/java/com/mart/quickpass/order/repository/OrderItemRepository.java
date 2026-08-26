package com.mart.quickpass.order.repository;

import com.mart.quickpass.order.entity.OrderItem;
import com.mart.quickpass.gate.dto.GateItemRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @EntityGraph(attributePaths = "product")
    // 조회 시 product도 함께 가져온다
    List<OrderItem> findAllByOrderIdOrderByIdAsc(Long orderId);

    @Query("""
            select new com.mart.quickpass.gate.dto.GateItemRow(oi.product.barcode, oi.quantity)
            from OrderItem oi
            where oi.order.id = :orderId
            order by oi.id asc
            """)
    List<GateItemRow> findGateItemsByOrderId(@Param("orderId") Long orderId);
}
