package com.mart.quickpass.order.repository;

import com.mart.quickpass.order.entity.Order;
import com.mart.quickpass.order.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Collection;

public interface OrderRepository extends JpaRepository<Order, Long> {

    boolean existsByUserIdAndStatusIn(Long userId, Collection<OrderStatus> statuses);

    Optional<Order> findByOrderId(String orderId);

    Optional<Order> findByUserIdAndCartIdAndStatus(Long userId, Long cartId, OrderStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select o from Order o
            where o.user.id = :userId
              and o.cart.id = :cartId
              and o.status = :status
            """)
    Optional<Order> findByUserAndCartAndStatusForUpdate(
            @Param("userId") Long userId,
            @Param("cartId") Long cartId,
            @Param("status") OrderStatus status);

    boolean existsByCartQrCodeAndStatus(String qrCode, OrderStatus status);

    @EntityGraph(attributePaths = "user")
    @Query(value = """
            select o from Order o
            where (:status is null or o.status = :status)
              and (:keyword is null
                   or lower(o.orderId) like lower(concat('%', :keyword, '%'))
                   or lower(o.orderName) like lower(concat('%', :keyword, '%'))
                   or lower(o.user.email) like lower(concat('%', :keyword, '%'))
                   or lower(o.user.name) like lower(concat('%', :keyword, '%')))
            """,
            countQuery = """
            select count(o) from Order o
            where (:status is null or o.status = :status)
              and (:keyword is null
                   or lower(o.orderId) like lower(concat('%', :keyword, '%'))
                   or lower(o.orderName) like lower(concat('%', :keyword, '%'))
                   or lower(o.user.email) like lower(concat('%', :keyword, '%'))
                   or lower(o.user.name) like lower(concat('%', :keyword, '%')))
            """)
    Page<Order> searchForAdmin(
            @Param("keyword") String keyword,
            @Param("status") OrderStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "user")
    @Query("select o from Order o where o.orderId = :orderId")
    Optional<Order> findByOrderIdWithUser(@Param("orderId") String orderId);


    // 승인 중인 주문을 잠가 여러 결제 시도가 동시에 같은 주문을 PAID로 만들지 못하게 한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.orderId = :orderId")
    Optional<Order> findByOrderIdForUpdate(@Param("orderId") String orderId);
}
