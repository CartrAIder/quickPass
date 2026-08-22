package com.mart.quickpass.order.entity;

import com.mart.quickpass.cart.entity.Cart;
import com.mart.quickpass.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "orders",
        uniqueConstraints = @UniqueConstraint(name = "uk_orders_order_id", columnNames = "order_id")
)
@Check(constraints = "total_amount > 0")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 기본키

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId; // 주문 번호

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_orders_user")
    )
    private User user; // 유저

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "cart_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_orders_cart")
    )
    private Cart cart; // 주문 스냅샷을 생성한 카트

    @Column(name = "order_name", nullable = false, length = 100)
    private String orderName; // 주문 이름

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount; // 주문의 총 금액

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status; // 상태


    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 주문 생성 시간

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 주문 업데이트 시간

    @Version
    @Column(nullable = false)
    private Long version; // 버전 번호(낙관적 락)

    @Builder
    public Order(
            String orderId,
            User user,
            Cart cart,
            String orderName,
            Long totalAmount,
            OrderStatus status,
            LocalDateTime expiresAt
    ) {
        this.orderId = orderId;
        this.user = user;
        this.cart = cart;
        this.orderName = orderName;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    public void markPaid() {
        this.status = OrderStatus.PAID;
    }

    public void expire() {
        this.status = OrderStatus.EXPIRED;
    }
}
