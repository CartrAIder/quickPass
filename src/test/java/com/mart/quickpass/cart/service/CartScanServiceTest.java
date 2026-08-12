package com.mart.quickpass.cart.service;

import com.mart.quickpass.cart.dto.CartScanMessage;
import com.mart.quickpass.cart.dto.CartSession;
import com.mart.quickpass.cart.entity.Cart;
import com.mart.quickpass.cart.repository.CartItemsRepository;
import com.mart.quickpass.cart.repository.CartRepository;
import com.mart.quickpass.cart.repository.CartScanDeduplicationRepository;
import com.mart.quickpass.cart.repository.CartSessionRepository;
import com.mart.quickpass.cart.repository.CartVersionRepository;
import com.mart.quickpass.global.config.CartSessionProperties;
import com.mart.quickpass.product.entity.Product;
import com.mart.quickpass.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartScanServiceTest {

    private final CartRepository cartRepository = mock(CartRepository.class);
    private final CartSessionRepository cartSessionRepository = mock(CartSessionRepository.class);
    private final CartItemsRepository cartItemsRepository = mock(CartItemsRepository.class);
    private final CartVersionRepository cartVersionRepository = mock(CartVersionRepository.class);
    private final CartScanDeduplicationRepository scanDeduplicationRepository =
            mock(CartScanDeduplicationRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final CartSessionProperties cartSessionProperties = new CartSessionProperties(Duration.ofHours(2));
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final CartScanService cartScanService = new CartScanService(
            cartRepository,
            cartSessionRepository,
            cartItemsRepository,
            cartVersionRepository,
            scanDeduplicationRepository,
            productRepository,
            cartSessionProperties,
            eventPublisher);

    @Test
    void duplicateScanIdDoesNotChangeCartQuantity() {
        CartScanMessage scan = new CartScanMessage("scan-1", "8801234567890", 1786430943000L);
        when(cartRepository.findByQrCode("cart_001")).thenReturn(Optional.of(mock(Cart.class)));
        when(cartSessionRepository.findByQrCode("cart_001")).thenReturn(Optional.of(CartSession.start(1L)));
        when(productRepository.findByBarcode("8801234567890")).thenReturn(Optional.of(mock(Product.class)));
        when(scanDeduplicationRepository.tryMarkProcessed("cart_001", "scan-1", Duration.ofHours(2)))
                .thenReturn(false);

        cartScanService.handleScan("cart_001", scan);

        verify(cartItemsRepository, never()).saveItem(any(), any(), any());
        verify(cartVersionRepository, never()).increment(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void scanWithoutScanIdIsIgnoredBeforeAnyLookup() {
        CartScanMessage scan = new CartScanMessage(" ", "8801234567890", 1786430943000L);

        cartScanService.handleScan("cart_001", scan);

        verify(cartRepository, never()).findByQrCode(eq("cart_001"));
        verify(scanDeduplicationRepository, never()).tryMarkProcessed(any(), any(), any());
    }
}
