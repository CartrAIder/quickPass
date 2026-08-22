package com.mart.quickpass.order.dto;

public record OrderCreateResult(
        OrderCreateResponse response,
        boolean created
) {
    public static OrderCreateResult created(OrderCreateResponse response) {
        return new OrderCreateResult(response, true);
    }

    public static OrderCreateResult existing(OrderCreateResponse response) {
        return new OrderCreateResult(response, false);
    }
}
