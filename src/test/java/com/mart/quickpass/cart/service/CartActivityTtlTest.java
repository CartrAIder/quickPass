package com.mart.quickpass.cart.service;

import com.mart.quickpass.cart.dto.CartItem;
import com.mart.quickpass.cart.dto.CartScanMessage;
import com.mart.quickpass.cart.dto.CartSession;
import com.mart.quickpass.cart.entity.Cart;
import com.mart.quickpass.cart.repository.CartItemsRepository;
import com.mart.quickpass.cart.repository.CartRepository;
import com.mart.quickpass.cart.repository.CartSessionRepository;
import com.mart.quickpass.cart.repository.CartVersionRepository;
import com.mart.quickpass.global.config.CartSessionProperties;
import com.mart.quickpass.global.exception.InvalidCartItemQuantityException;
import com.mart.quickpass.product.entity.Product;
import com.mart.quickpass.product.entity.ProductStatus;
import com.mart.quickpass.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartActivityTtlTest {

    private static final Long USER_ID = 42L;
    private static final String QR_CODE = "CART-001";
    private static final String BARCODE = "8801234567890";
    private static final Duration TTL = Duration.ofHours(2);

    @Test
    void refreshesUserCartTtlWhenUserAdjustsItem() {
        CartSessionRepository sessionRepository = mock(CartSessionRepository.class);
        CartItemsRepository itemsRepository = mock(CartItemsRepository.class);
        CartStateChangePublisher stateChangePublisher = mock(CartStateChangePublisher.class);
        CartSessionGuard sessionGuard = mock(CartSessionGuard.class);
        CartItemService service = new CartItemService(
                sessionRepository,
                itemsRepository,
                new CartSessionProperties(TTL),
                sessionGuard,
                stateChangePublisher);

        when(sessionGuard.requireOwnedSession(USER_ID, QR_CODE)).thenReturn(CartSession.start(USER_ID));
        when(itemsRepository.findItem(QR_CODE, BARCODE))
                .thenReturn(Optional.of(new CartItem("생수", 1_000, 1)));
        Optional<CartItem> result = service.adjustQuantity(USER_ID, QR_CODE, BARCODE, 2);

        assertThat(result).contains(new CartItem("생수", 1_000, 2));
        verify(itemsRepository).saveItem(QR_CODE, BARCODE, new CartItem("생수", 1_000, 2));
        verify(sessionRepository).refreshSessionTtl(USER_ID, QR_CODE, TTL);
        verify(stateChangePublisher).publish(USER_ID, QR_CODE, com.mart.quickpass.cart.event.CartChangeType.UPDATED);
    }

    @Test
    void removesItemWhenQuantityIsZero() {
        CartSessionRepository sessionRepository = mock(CartSessionRepository.class);
        CartItemsRepository itemsRepository = mock(CartItemsRepository.class);
        CartSessionGuard sessionGuard = mock(CartSessionGuard.class);
        CartStateChangePublisher stateChangePublisher = mock(CartStateChangePublisher.class);
        CartItemService service = new CartItemService(
                sessionRepository,
                itemsRepository,
                new CartSessionProperties(TTL),
                sessionGuard,
                stateChangePublisher);

        CartItem item = new CartItem("생수", 1_000, 1);
        when(sessionGuard.requireOwnedSession(USER_ID, QR_CODE)).thenReturn(CartSession.start(USER_ID));
        when(itemsRepository.findItem(QR_CODE, BARCODE)).thenReturn(Optional.of(item));

        Optional<CartItem> result = service.adjustQuantity(USER_ID, QR_CODE, BARCODE, 0);

        assertThat(result).isEmpty();
        verify(itemsRepository).deleteItem(QR_CODE, BARCODE);
        verify(sessionRepository).refreshSessionTtl(USER_ID, QR_CODE, TTL);
        verify(stateChangePublisher).publish(USER_ID, QR_CODE, com.mart.quickpass.cart.event.CartChangeType.UPDATED);
    }

    @Test
    void rejectsNegativeQuantity() {
        CartSessionRepository sessionRepository = mock(CartSessionRepository.class);
        CartItemsRepository itemsRepository = mock(CartItemsRepository.class);
        CartSessionGuard sessionGuard = mock(CartSessionGuard.class);
        CartStateChangePublisher stateChangePublisher = mock(CartStateChangePublisher.class);
        CartItemService service = new CartItemService(
                sessionRepository,
                itemsRepository,
                new CartSessionProperties(TTL),
                sessionGuard,
                stateChangePublisher);

        assertThatThrownBy(() -> service.adjustQuantity(USER_ID, QR_CODE, BARCODE, -1))
                .isInstanceOf(InvalidCartItemQuantityException.class);

        verify(sessionGuard, never()).requireOwnedSession(any(), any());
        verify(itemsRepository, never()).saveItem(any(), any(), any());
        verify(itemsRepository, never()).deleteItem(any(), any());
    }

    @Test
    void refreshesUserCartTtlWhenCartScansItem() {
        CartRepository cartRepository = mock(CartRepository.class);
        CartSessionRepository sessionRepository = mock(CartSessionRepository.class);
        CartItemsRepository itemsRepository = mock(CartItemsRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        CartStateChangePublisher stateChangePublisher = mock(CartStateChangePublisher.class);
        CartScanService service = new CartScanService(
                cartRepository,
                sessionRepository,
                itemsRepository,
                productRepository,
                new CartSessionProperties(TTL),
                stateChangePublisher);
        Product product = Product.builder()
                .barcode(BARCODE)
                .name("생수")
                .price(1_000)
                .category("음료")
                .status(ProductStatus.ON_SALE)
                .build();

        when(cartRepository.findByQrCode(QR_CODE))
                .thenReturn(Optional.of(mock(Cart.class)), Optional.empty());
        when(sessionRepository.findByQrCode(QR_CODE)).thenReturn(Optional.of(CartSession.start(USER_ID)));
        when(productRepository.findByBarcode(BARCODE)).thenReturn(Optional.of(product));
        when(itemsRepository.findItem(QR_CODE, BARCODE)).thenReturn(Optional.empty());
        service.handleScan(QR_CODE, new CartScanMessage(BARCODE, 1L));

        verify(sessionRepository).refreshSessionTtl(USER_ID, QR_CODE, TTL);
        verify(stateChangePublisher).publish(USER_ID, QR_CODE, com.mart.quickpass.cart.event.CartChangeType.UPDATED);
    }
}
