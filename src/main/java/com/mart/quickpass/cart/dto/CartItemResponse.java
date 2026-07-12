package com.mart.quickpass.cart.dto;

public record CartItemResponse(
        String barcode,
        String name,
        int price,
        long quantity
) {

    public static CartItemResponse from(String barcode, CartItem item) {
        return new CartItemResponse(barcode, item.name(), item.price(), item.quantity());
    }
}
