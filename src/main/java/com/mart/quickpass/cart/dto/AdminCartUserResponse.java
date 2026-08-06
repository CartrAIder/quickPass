package com.mart.quickpass.cart.dto;

import com.mart.quickpass.user.entity.User;

public record AdminCartUserResponse(
        Long userId,
        String name
) {

    public static AdminCartUserResponse from(User user) {
        return new AdminCartUserResponse(user.getId(), user.getName());
    }
}
