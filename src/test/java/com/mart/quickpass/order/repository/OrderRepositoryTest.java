package com.mart.quickpass.order.repository;

import com.mart.quickpass.cart.entity.Cart;
import com.mart.quickpass.cart.entity.CartStatus;
import com.mart.quickpass.cart.repository.CartRepository;
import com.mart.quickpass.order.entity.Order;
import com.mart.quickpass.order.entity.OrderStatus;
import com.mart.quickpass.user.entity.User;
import com.mart.quickpass.user.entity.UserRole;
import com.mart.quickpass.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Test
    void purchaseHistoryIncludesOnlyOwnedPaidAndCanceledOrdersInRecentPurchaseOrder() {
        User owner = userRepository.save(user("history-owner@example.com"));
        User other = userRepository.save(user("history-other@example.com"));
        Cart cart = cartRepository.save(Cart.builder()
                .qrCode("history_cart_001")
                .status(CartStatus.WAITING)
                .build());
        LocalDateTime older = LocalDateTime.of(2026, 8, 20, 10, 0);
        LocalDateTime newer = LocalDateTime.of(2026, 8, 21, 10, 0);

        orderRepository.saveAll(List.of(
                order("history-paid", owner, cart, OrderStatus.PAID, older),
                order("history-canceled", owner, cart, OrderStatus.CANCELED, newer),
                order("history-pending", owner, cart, OrderStatus.PENDING_PAYMENT, null),
                order("history-expired", owner, cart, OrderStatus.EXPIRED, null),
                order("history-other-user", other, cart, OrderStatus.PAID, newer.plusDays(1))
        ));
        Sort sort = Sort.by(Sort.Direction.DESC, "paidAt")
                .and(Sort.by(Sort.Direction.DESC, "id"));

        Slice<Order> firstPage = orderRepository.findPurchaseHistory(
                owner.getId(),
                List.of(OrderStatus.PAID, OrderStatus.CANCELED),
                PageRequest.of(0, 1, sort)
        );
        Slice<Order> secondPage = orderRepository.findPurchaseHistory(
                owner.getId(),
                List.of(OrderStatus.PAID, OrderStatus.CANCELED),
                PageRequest.of(1, 1, sort)
        );

        assertThat(firstPage.getContent())
                .extracting(Order::getOrderId)
                .containsExactly("history-canceled");
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.getContent())
                .extracting(Order::getOrderId)
                .containsExactly("history-paid");
        assertThat(secondPage.hasNext()).isFalse();
    }

    private User user(String email) {
        return User.builder()
                .email(email)
                .password("encoded")
                .name("구매내역 사용자")
                .role(UserRole.USER)
                .build();
    }

    private Order order(
            String orderId,
            User user,
            Cart cart,
            OrderStatus status,
            LocalDateTime paidAt
    ) {
        return Order.builder()
                .orderId(orderId)
                .user(user)
                .cart(cart)
                .orderName("테스트 상품")
                .totalAmount(1000L)
                .status(status)
                .paidAt(paidAt)
                .build();
    }
}
