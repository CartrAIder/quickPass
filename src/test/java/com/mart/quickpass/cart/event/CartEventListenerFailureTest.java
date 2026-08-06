package com.mart.quickpass.cart.event;

import com.mart.quickpass.cart.service.CartAdminService;
import com.mart.quickpass.cart.service.CartSnapshotService;
import com.mart.quickpass.cart.sse.AdminCartSseService;
import com.mart.quickpass.cart.sse.CartSseService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CartEventListenerFailureTest {

    private static final CartChangedEvent EVENT =
            CartChangedEvent.of(42L, "CART-001", CartChangeType.UPDATED, 10L);

    @Test
    void userProjectionFailureDoesNotPropagateToBusinessRequest() {
        CartSnapshotService snapshotService = mock(CartSnapshotService.class);
        when(snapshotService.snapshot("CART-001", 10L))
                .thenThrow(new IllegalStateException("snapshot failed"));
        CartEventListener listener = new CartEventListener(snapshotService, mock(CartSseService.class));

        assertThatCode(() -> listener.onCartChanged(EVENT)).doesNotThrowAnyException();
    }

    @Test
    void adminProjectionFailureDoesNotPropagateToBusinessRequest() {
        CartAdminService adminService = mock(CartAdminService.class);
        when(adminService.getCart("CART-001"))
                .thenThrow(new IllegalStateException("admin projection failed"));
        AdminCartEventListener listener = new AdminCartEventListener(
                adminService, mock(AdminCartSseService.class));

        assertThatCode(() -> listener.onCartChanged(EVENT)).doesNotThrowAnyException();
    }
}
