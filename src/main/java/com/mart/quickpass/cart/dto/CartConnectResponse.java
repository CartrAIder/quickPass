package com.mart.quickpass.cart.dto;

import com.mart.quickpass.cart.entity.Cart;

public record CartConnectResponse(
        Long cartId,
        String qrCode,
        AdminCartStatus status,
        CartConnectionType connectionType,
        CartSnapshotResponse snapshot
) {

    public static CartConnectResponse of(
            Cart cart,
            CartConnectionType connectionType,
            CartSnapshotResponse snapshot
    ) {
        return new CartConnectResponse(
                cart.getId(), cart.getQrCode(), AdminCartStatus.IN_USE, connectionType, snapshot);
    }
}
