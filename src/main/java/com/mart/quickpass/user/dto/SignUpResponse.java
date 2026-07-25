package com.mart.quickpass.user.dto;

import com.mart.quickpass.user.entity.User;
import com.mart.quickpass.user.entity.UserRole;

public record SignUpResponse(
        Long id,
        String email,
        String name,
        UserRole role
) {

    public static SignUpResponse from(User user) {
        return new SignUpResponse(user.getId(), user.getEmail(), user.getName(), user.getRole());
    }
}
