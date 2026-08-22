package com.mart.quickpass.cart.dto;

import com.mart.quickpass.cart.entity.Cart;
import com.mart.quickpass.cart.entity.CartStatus;

public record CartConnectResponse(
        Long cartId,
        String qrCode,
        CartStatus status,
        CartConnectionType connectionType,
        CartSnapshotResponse snapshot,
        CheckoutStatus checkoutStatus,
        PendingOrderResponse pendingOrder
) {

    public static CartConnectResponse of(
            Cart cart,
            CartConnectionType connectionType,
            CartSnapshotResponse snapshot,
            PendingOrderResponse pendingOrder
    ) {
        return new CartConnectResponse(
                cart.getId(),
                cart.getQrCode(),
                cart.getStatus(),
                connectionType,
                snapshot,
                pendingOrder == null ? CheckoutStatus.SHOPPING : CheckoutStatus.PAYMENT_PENDING,
                pendingOrder);
    }
}
