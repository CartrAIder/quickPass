package com.mart.quickpass.cart.dto;

import com.mart.quickpass.cart.entity.Cart;
import com.mart.quickpass.cart.entity.CartStatus;

public record CartConnectResponse(
        Long cartId,
        String qrCode,
        CartStatus status
) {

    public static CartConnectResponse from(Cart cart) {
        return new CartConnectResponse(cart.getId(), cart.getQrCode(), cart.getStatus());
    }
}
