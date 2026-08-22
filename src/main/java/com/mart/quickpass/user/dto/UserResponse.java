package com.mart.quickpass.user.dto;

public record UserResponse(
        Long userId,
        String email,
        String name
) {
}
