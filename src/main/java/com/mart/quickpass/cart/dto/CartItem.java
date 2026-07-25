package com.mart.quickpass.cart.dto;


public record CartItem(
        // 상품 dto
        String name,
        int price,
        long quantity
) {

    public CartItem incrementQuantity(long delta) {
        return new CartItem(name, price, quantity + delta);
    }
}
