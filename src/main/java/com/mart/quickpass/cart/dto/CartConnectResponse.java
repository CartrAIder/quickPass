package com.mart.quickpass.cart.dto;

import com.mart.quickpass.cart.entity.Cart;
import com.mart.quickpass.cart.entity.CartStatus;

public record CartConnectResponse(
        Long cartId,
        String qrCode,
        CartStatus status,
        CartConnectionType connectionType,
        CartSnapshotResponse snapshot
) {

    public static CartConnectResponse of(
            Cart cart,
            CartConnectionType connectionType,
            CartSnapshotResponse snapshot
    ) {
        return new CartConnectResponse(
                cart.getId(), cart.getQrCode(), cart.getStatus(), connectionType, snapshot);
    }
}
