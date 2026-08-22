package com.mart.quickpass.cart.service;

import com.mart.quickpass.cart.dto.CartConnectionType;
import com.mart.quickpass.cart.dto.CartSession;
import com.mart.quickpass.cart.dto.CartSnapshotResponse;
import com.mart.quickpass.cart.dto.CheckoutStatus;
import com.mart.quickpass.cart.entity.Cart;
import com.mart.quickpass.cart.entity.CartStatus;
import com.mart.quickpass.cart.event.CartChangeType;
import com.mart.quickpass.cart.event.CartChangedEvent;
import com.mart.quickpass.cart.repository.CartItemsRepository;
import com.mart.quickpass.cart.repository.CartRepository;
import com.mart.quickpass.cart.repository.CartSessionRepository;
import com.mart.quickpass.cart.repository.CartVersionRepository;
import com.mart.quickpass.global.config.CartSessionProperties;
import com.mart.quickpass.global.exception.CartPaymentPendingException;
import com.mart.quickpass.order.entity.Order;
import com.mart.quickpass.order.entity.OrderStatus;
import com.mart.quickpass.order.repository.OrderRepository;
import com.mart.quickpass.user.entity.User;
import com.mart.quickpass.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartConnectionServiceTest {

    private final CartRepository cartRepository = mock(CartRepository.class);
    private final CartSessionRepository cartSessionRepository = mock(CartSessionRepository.class);
    private final CartItemsRepository cartItemsRepository = mock(CartItemsRepository.class);
    private final CartVersionRepository cartVersionRepository = mock(CartVersionRepository.class);
    private final CartSessionProperties properties = new CartSessionProperties(Duration.ofHours(2));
    private final CartSessionGuard cartSessionGuard = mock(CartSessionGuard.class);
    private final CartSnapshotService cartSnapshotService = mock(CartSnapshotService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);

    private CartConnectionService service;
    private Cart cart;

    @BeforeEach
    void setUp() {
        service = new CartConnectionService(
                cartRepository,
                cartSessionRepository,
                cartItemsRepository,
                cartVersionRepository,
                properties,
                cartSessionGuard,
                cartSnapshotService,
                eventPublisher,
                orderRepository);
        cart = Cart.builder().qrCode("cart_001").status(CartStatus.IN_USE).build();
        ReflectionTestUtils.setField(cart, "id", 10L);
        when(cartSessionRepository.findQrCodeByUserId(1L)).thenReturn(Optional.of("cart_001"));
        when(cartSessionRepository.findByQrCode("cart_001")).thenReturn(Optional.of(CartSession.start(1L)));
        when(cartRepository.findByQrCode("cart_001")).thenReturn(Optional.of(cart));
        when(cartVersionRepository.current("cart_001")).thenReturn(3L);
        when(cartSnapshotService.snapshot("cart_001", 3L)).thenReturn(mock(CartSnapshotResponse.class));
    }

    @Test
    void currentCartIsShoppingWhenPendingOrderDoesNotExist() {
        when(orderRepository.findByUserIdAndCartIdAndStatus(1L, 10L, OrderStatus.PENDING_PAYMENT))
                .thenReturn(Optional.empty());

        var response = service.current(1L).orElseThrow();

        assertThat(response.checkoutStatus()).isEqualTo(CheckoutStatus.SHOPPING);
        assertThat(response.pendingOrder()).isNull();
    }

    @Test
    void currentCartRestoresPendingOrder() {
        User user = User.builder()
                .email("user@example.com")
                .password("encoded")
                .name("사용자")
                .role(UserRole.USER)
                .build();
        Order order = Order.builder()
                .orderId("pending-order")
                .user(user)
                .cart(cart)
                .orderName("우유 외 1건")
                .totalAmount(7500L)
                .status(OrderStatus.PENDING_PAYMENT)
                .build();
        when(orderRepository.findByUserIdAndCartIdAndStatus(1L, 10L, OrderStatus.PENDING_PAYMENT))
                .thenReturn(Optional.of(order));

        var response = service.current(1L).orElseThrow();

        assertThat(response.checkoutStatus()).isEqualTo(CheckoutStatus.PAYMENT_PENDING);
        assertThat(response.pendingOrder().orderId()).isEqualTo("pending-order");
        assertThat(response.pendingOrder().orderName()).isEqualTo("우유 외 1건");
        assertThat(response.pendingOrder().amount()).isEqualTo(7500L);
        assertThat(response.connectionType()).isEqualTo(CartConnectionType.RESUMED);
    }

    @Test
    void currentCartCannotBeDisconnectedWhilePaymentIsPending() {
        doThrow(new CartPaymentPendingException())
                .when(cartSessionGuard).requireShoppingSession(1L, "cart_001");

        assertThatThrownBy(() -> service.disconnect(1L, "cart_001"))
                .isInstanceOf(CartPaymentPendingException.class);

        verify(cartItemsRepository, never()).deleteAll("cart_001");
        verify(cartSessionRepository, never()).deleteByQrCode("cart_001");
    }

    @Test
    void paymentCompletionClearsCartOwnershipAndPublishesClosedEvent() {
        when(cartVersionRepository.increment("cart_001")).thenReturn(4L);

        service.completePayment(1L, cart);

        assertThat(cart.getStatus()).isEqualTo(CartStatus.WAITING);
        verify(cartItemsRepository).deleteAll("cart_001");
        verify(cartSessionRepository).deleteByQrCode("cart_001");
        verify(cartSessionRepository).deleteUserCart(1L);
        ArgumentCaptor<CartChangedEvent> eventCaptor = ArgumentCaptor.forClass(CartChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().type()).isEqualTo(CartChangeType.CLOSED);
        assertThat(eventCaptor.getValue().version()).isEqualTo(4L);
    }
}
