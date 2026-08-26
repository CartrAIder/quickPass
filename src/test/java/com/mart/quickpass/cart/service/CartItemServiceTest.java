package com.mart.quickpass.cart.service;

import com.mart.quickpass.cart.dto.CartSession;
import com.mart.quickpass.cart.event.CartChangedEvent;
import com.mart.quickpass.cart.repository.CartItemsRepository;
import com.mart.quickpass.cart.repository.CartRepository;
import com.mart.quickpass.cart.repository.CartSessionRepository;
import com.mart.quickpass.cart.repository.CartVersionRepository;
import com.mart.quickpass.global.config.CartSessionProperties;
import com.mart.quickpass.global.exception.CartPaymentPendingException;
import com.mart.quickpass.order.entity.OrderStatus;
import com.mart.quickpass.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartItemServiceTest {

    private final CartSessionRepository cartSessionRepository = mock(CartSessionRepository.class);
    private final CartItemsRepository cartItemsRepository = mock(CartItemsRepository.class);
    private final CartRepository cartRepository = mock(CartRepository.class);
    private final CartVersionRepository cartVersionRepository = mock(CartVersionRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final CartSessionGuard guard = new CartSessionGuard(
            cartSessionRepository, cartRepository, orderRepository);
    private final CartItemService service = new CartItemService(
            cartSessionRepository,
            cartItemsRepository,
            cartVersionRepository,
            new CartSessionProperties(Duration.ofHours(2)),
            guard,
            eventPublisher);

    @Test
    void itemQuantityCannotChangeWhilePaymentIsPending() {
        when(cartSessionRepository.findByQrCode("cart_001"))
                .thenReturn(Optional.of(CartSession.start(1L)));
        when(cartRepository.findByQrCodeForUpdate("cart_001"))
                .thenReturn(Optional.of(mock(com.mart.quickpass.cart.entity.Cart.class)));
        when(orderRepository.existsByCartQrCodeAndStatus("cart_001", OrderStatus.PENDING_PAYMENT))
                .thenReturn(true);

        assertThatThrownBy(() -> service.adjustQuantity(1L, "cart_001", "barcode", 2))
                .isInstanceOf(CartPaymentPendingException.class);

        verify(cartItemsRepository, never()).saveItem(any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any(CartChangedEvent.class));
    }

    @Test
    void itemCannotBeRemovedWhilePaymentIsPending() {
        when(cartSessionRepository.findByQrCode("cart_001"))
                .thenReturn(Optional.of(CartSession.start(1L)));
        when(cartRepository.findByQrCodeForUpdate("cart_001"))
                .thenReturn(Optional.of(mock(com.mart.quickpass.cart.entity.Cart.class)));
        when(orderRepository.existsByCartQrCodeAndStatus("cart_001", OrderStatus.PENDING_PAYMENT))
                .thenReturn(true);

        assertThatThrownBy(() -> service.removeItem(1L, "cart_001", "barcode"))
                .isInstanceOf(CartPaymentPendingException.class);

        verify(cartItemsRepository, never()).deleteItem(any(), any());
    }
}
