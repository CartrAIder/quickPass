package com.mart.quickpass.cart.service;

import com.mart.quickpass.cart.dto.AdminCartResponse;
import com.mart.quickpass.cart.dto.CartItem;
import com.mart.quickpass.cart.dto.CartSession;
import com.mart.quickpass.cart.dto.CartSnapshotResponse;
import com.mart.quickpass.cart.entity.Cart;
import com.mart.quickpass.cart.entity.CartStatus;
import com.mart.quickpass.cart.repository.CartRepository;
import com.mart.quickpass.cart.repository.CartItemsRepository;
import com.mart.quickpass.cart.repository.CartSessionRepository;
import com.mart.quickpass.cart.repository.CartVersionRepository;
import com.mart.quickpass.cart.sse.CartSseService;
import com.mart.quickpass.cart.event.CartChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.mart.quickpass.user.entity.User;
import com.mart.quickpass.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartAdminServiceTest {

    @Test
    void createsAdminCartListFromDatabaseAndRedisState() {
        CartRepository cartRepository = mock(CartRepository.class);
        CartSessionRepository sessionRepository = mock(CartSessionRepository.class);
        CartItemsRepository itemsRepository = mock(CartItemsRepository.class);
        CartVersionRepository versionRepository = mock(CartVersionRepository.class);
        CartSnapshotService snapshotService = mock(CartSnapshotService.class);
        UserRepository userRepository = mock(UserRepository.class);
        CartAdminService service = new CartAdminService(
                cartRepository, itemsRepository, sessionRepository, versionRepository, snapshotService, userRepository,
                mock(CartSseService.class),
                mock(CartStateChangePublisher.class));

        Cart available = Cart.builder().qrCode("cart-001").status(CartStatus.AVAILABLE).build();
        Cart inUse = Cart.builder().qrCode("cart-002").status(CartStatus.AVAILABLE).build();
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getName()).thenReturn("홍길동");

        when(cartRepository.findAll()).thenReturn(List.of(available, inUse));
        when(sessionRepository.findByQrCode("cart-001")).thenReturn(Optional.empty());
        when(sessionRepository.findByQrCode("cart-002"))
                .thenReturn(Optional.of(CartSession.start(1L)));
        when(userRepository.findAllById(List.of(1L))).thenReturn(List.of(user));
        when(versionRepository.current("cart-001")).thenReturn(0L);
        when(versionRepository.current("cart-002")).thenReturn(3L);
        when(snapshotService.snapshot("cart-001", 0L))
                .thenReturn(CartSnapshotResponse.of("cart-001", 0L, Map.of()));
        when(snapshotService.snapshot("cart-002", 3L)).thenReturn(CartSnapshotResponse.of(
                "cart-002", 3L, Map.of("123", new CartItem("우유", 2_000, 2))));

        List<AdminCartResponse> result = service.getCartList();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).status()).hasToString("AVAILABLE");
        assertThat(result.get(0).version()).isZero();
        assertThat(result.get(0).currentUser()).isNull();
        assertThat(result.get(1).status()).hasToString("IN_USE");
        assertThat(result.get(1).version()).isEqualTo(3L);
        assertThat(result.get(1).currentUser().name()).isEqualTo("홍길동");
        assertThat(result.get(1).productCount()).isEqualTo(2);
        assertThat(result.get(1).totalAmount()).isEqualTo(4_000);
    }

    @Test
    void getsCartDetailWithConnectionAndBasketSnapshot() {
        CartRepository cartRepository = mock(CartRepository.class);
        CartSessionRepository sessionRepository = mock(CartSessionRepository.class);
        CartItemsRepository itemsRepository = mock(CartItemsRepository.class);
        CartVersionRepository versionRepository = mock(CartVersionRepository.class);
        CartSnapshotService snapshotService = mock(CartSnapshotService.class);
        UserRepository userRepository = mock(UserRepository.class);
        CartAdminService service = new CartAdminService(
                cartRepository, itemsRepository, sessionRepository, versionRepository, snapshotService, userRepository,
                mock(CartSseService.class),
                mock(CartStateChangePublisher.class));
        Cart cart = mock(Cart.class);
        User user = mock(User.class);

        when(cart.getId()).thenReturn(10L);
        when(cart.getQrCode()).thenReturn("cart-010");
        when(cart.getStatus()).thenReturn(CartStatus.AVAILABLE);
        when(cartRepository.findById(10L)).thenReturn(Optional.of(cart));
        when(sessionRepository.findByQrCode("cart-010")).thenReturn(Optional.of(CartSession.start(1L)));
        when(user.getId()).thenReturn(1L);
        when(user.getName()).thenReturn("홍길동");
        when(userRepository.findAllById(List.of(1L))).thenReturn(List.of(user));
        when(versionRepository.current("cart-010")).thenReturn(5L);
        when(snapshotService.snapshot("cart-010", 5L)).thenReturn(CartSnapshotResponse.of(
                "cart-010", 5L, Map.of("123", new CartItem("우유", 2_000, 2))));

        var result = service.getCartDetail(10L);

        assertThat(result.cartId()).isEqualTo(10L);
        assertThat(result.version()).isEqualTo(5L);
        assertThat(result.currentUser().name()).isEqualTo("홍길동");
        assertThat(result.connectedAt()).isNotNull();
        assertThat(result.items()).hasSize(1);
        assertThat(result.totalQuantity()).isEqualTo(2);
        assertThat(result.totalAmount()).isEqualTo(4_000);
    }

    @Test
    void forceDisconnectClearsSessionAndTerminatesUserStream() {
        CartRepository cartRepository = mock(CartRepository.class);
        CartItemsRepository itemsRepository = mock(CartItemsRepository.class);
        CartSessionRepository sessionRepository = mock(CartSessionRepository.class);
        CartVersionRepository versionRepository = mock(CartVersionRepository.class);
        CartSseService cartSseService = mock(CartSseService.class);
        CartStateChangePublisher stateChangePublisher = mock(CartStateChangePublisher.class);
        CartAdminService service = new CartAdminService(
                cartRepository, itemsRepository, sessionRepository, versionRepository,
                mock(CartSnapshotService.class), mock(UserRepository.class), cartSseService,
                stateChangePublisher);
        Cart cart = mock(Cart.class);
        when(cart.getId()).thenReturn(10L);
        when(cart.getQrCode()).thenReturn("cart-010");
        when(cartRepository.findById(10L)).thenReturn(Optional.of(cart));
        when(sessionRepository.findByQrCode("cart-010")).thenReturn(Optional.of(CartSession.start(1L)));
        service.forceDisconnect(10L);

        verify(itemsRepository).deleteAll("cart-010");
        verify(sessionRepository).deleteByQrCode("cart-010");
        verify(sessionRepository).deleteUserCart(1L);
        verify(stateChangePublisher).publish(1L, "cart-010", com.mart.quickpass.cart.event.CartChangeType.CLOSED);
        verify(cartSseService).disconnect(1L);
    }
}
