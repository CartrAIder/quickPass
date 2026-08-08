package com.mart.quickpass.order.repository;

import com.mart.quickpass.order.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Collection;
import com.mart.quickpass.order.entity.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {

    boolean existsByUserIdAndStatusIn(Long userId, Collection<OrderStatus> statuses);

    Optional<Order> findByOrderId(String orderId);

    /**
     * 승인 중인 주문을 잠가 여러 결제 시도가 동시에 같은 주문을 PAID로 만들지 못하게 한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.orderId = :orderId")
    Optional<Order> findByOrderIdForUpdate(@Param("orderId") String orderId);
}
