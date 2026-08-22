package com.mart.quickpass.order.service;

import com.mart.quickpass.cart.dto.CartSession;
import com.mart.quickpass.cart.entity.Cart;
import com.mart.quickpass.cart.entity.CartStatus;
import com.mart.quickpass.cart.repository.CartRepository;
import com.mart.quickpass.cart.repository.CartSessionRepository;
import com.mart.quickpass.order.dto.OrderCreateItemRequest;
import com.mart.quickpass.order.dto.OrderCreateRequest;
import com.mart.quickpass.order.dto.OrderCreateResult;
import com.mart.quickpass.order.entity.Order;
import com.mart.quickpass.order.entity.OrderStatus;
import com.mart.quickpass.order.repository.OrderItemRepository;
import com.mart.quickpass.order.repository.OrderRepository;
import com.mart.quickpass.payment.repository.PaymentAttemptRepository;
import com.mart.quickpass.payment.entity.PaymentAttempt;
import com.mart.quickpass.payment.entity.PaymentStatus;
import com.mart.quickpass.product.entity.Product;
import com.mart.quickpass.product.entity.ProductCategory;
import com.mart.quickpass.product.entity.ProductStatus;
import com.mart.quickpass.product.repository.ProductRepository;
import com.mart.quickpass.user.entity.User;
import com.mart.quickpass.user.entity.UserRole;
import com.mart.quickpass.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final OrderItemRepository orderItemRepository = mock(OrderItemRepository.class);
    private final CartSessionRepository cartSessionRepository = mock(CartSessionRepository.class);
    private final CartRepository cartRepository = mock(CartRepository.class);
    private final PaymentAttemptRepository paymentAttemptRepository = mock(PaymentAttemptRepository.class);

    private OrderService orderService;
    private User user;
    private Cart cart;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                userRepository,
                productRepository,
                orderRepository,
                orderItemRepository,
                cartSessionRepository,
                cartRepository,
                paymentAttemptRepository);
        user = User.builder()
                .email("user@example.com")
                .password("encoded")
                .name("사용자")
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        cart = Cart.builder().qrCode("cart_001").status(CartStatus.IN_USE).build();
        ReflectionTestUtils.setField(cart, "id", 10L);

        when(cartSessionRepository.findQrCodeByUserId(1L)).thenReturn(Optional.of("cart_001"));
        when(cartSessionRepository.findByQrCode("cart_001")).thenReturn(Optional.of(CartSession.start(1L)));
        when(cartRepository.findByQrCodeForUpdate("cart_001")).thenReturn(Optional.of(cart));
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
    }

    @Test
    void createsPendingOrderForCurrentCartWhenNoneExists() {
        Product product = product(100L, "우유", 3000);
        OrderCreateRequest request = new OrderCreateRequest(List.of(new OrderCreateItemRequest(100L, 2)));
        when(orderRepository.findByUserAndCartAndStatusForUpdate(1L, 10L, OrderStatus.PENDING_PAYMENT))
                .thenReturn(Optional.empty());
        when(productRepository.findAllById(List.of(100L))).thenReturn(List.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderCreateResult result = orderService.create(1L, request);

        assertThat(result.created()).isTrue();
        assertThat(result.response().totalAmount()).isEqualTo(6000L);
        assertThat(result.response().status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        verify(orderItemRepository).saveAll(any());
    }

    @Test
    void returnsExistingPendingOrderWithoutCreatingAnotherSnapshot() {
        Order existing = Order.builder()
                .orderId("existing-order")
                .user(user)
                .cart(cart)
                .orderName("기존 주문")
                .totalAmount(4500L)
                .status(OrderStatus.PENDING_PAYMENT)
                .build();
        when(orderRepository.findByUserAndCartAndStatusForUpdate(1L, 10L, OrderStatus.PENDING_PAYMENT))
                .thenReturn(Optional.of(existing));

        OrderCreateResult result = orderService.create(
                1L, new OrderCreateRequest(List.of(new OrderCreateItemRequest(999L, 1))));

        assertThat(result.created()).isFalse();
        assertThat(result.response().orderId()).isEqualTo("existing-order");
        verify(productRepository, never()).findAllById(any());
        verify(orderRepository, never()).save(any());
        verify(orderItemRepository, never()).saveAll(any());
    }

    @Test
    void abandonsPendingOrderAndCancelsReadyPaymentAttempt() {
        Order order = pendingOrder("order-to-abandon");
        PaymentAttempt attempt = PaymentAttempt.builder()
                .paymentAttemptId("attempt-1")
                .order(order)
                .provider("TOSS_PAYMENTS")
                .requestedAmount(4500L)
                .status(PaymentStatus.READY)
                .build();
        when(orderRepository.findByOrderIdForUpdate("order-to-abandon")).thenReturn(Optional.of(order));
        when(paymentAttemptRepository.existsByOrder_IdAndStatusIn(
                order.getId(), List.of(PaymentStatus.APPROVED))).thenReturn(false);
        when(paymentAttemptRepository.findAllByOrder_IdAndStatus(order.getId(), PaymentStatus.READY))
                .thenReturn(List.of(attempt));

        orderService.abandon(1L, "order-to-abandon");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(attempt.getStatus()).isEqualTo(PaymentStatus.CANCELED);
    }

    @Test
    void doesNotAbandonOrderWithApprovedPayment() {
        Order order = pendingOrder("approved-order");
        when(orderRepository.findByOrderIdForUpdate("approved-order")).thenReturn(Optional.of(order));
        when(paymentAttemptRepository.existsByOrder_IdAndStatusIn(
                order.getId(), List.of(PaymentStatus.APPROVED))).thenReturn(true);

        assertThatThrownBy(() -> orderService.abandon(1L, "approved-order"))
                .hasMessageContaining("승인");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    private Order pendingOrder(String orderId) {
        Order order = Order.builder()
                .orderId(orderId)
                .user(user)
                .cart(cart)
                .orderName("기존 주문")
                .totalAmount(4500L)
                .status(OrderStatus.PENDING_PAYMENT)
                .build();
        ReflectionTestUtils.setField(order, "id", 20L);
        return order;
    }

    private Product product(Long id, String name, int price) {
        Product product = Product.builder()
                .barcode("barcode-" + id)
                .name(name)
                .price(price)
                .category(ProductCategory.DAIRY)
                .status(ProductStatus.ON_SALE)
                .build();
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }
}
