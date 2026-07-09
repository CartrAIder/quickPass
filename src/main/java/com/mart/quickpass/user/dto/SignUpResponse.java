package com.mart.quickpass.user.dto;

import com.mart.quickpass.user.entity.User;

public record SignUpResponse(
        Long id,
        String email,
        String name
) {

    public static SignUpResponse from(User user) {
        return new SignUpResponse(user.getId(), user.getEmail(), user.getName());
    }
}
