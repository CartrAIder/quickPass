package com.mart.quickpass.cart.dto;

/**
 * Redis Hash({@code cart:items:{qrCode}})의 필드({@code barcode}) 값.
 */
public record CartItem(
        String name,
        int price,
        long quantity
) {

    public CartItem incrementQuantity(long delta) {
        return new CartItem(name, price, quantity + delta);
    }
}
