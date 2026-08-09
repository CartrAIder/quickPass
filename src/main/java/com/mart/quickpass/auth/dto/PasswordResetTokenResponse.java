package com.mart.quickpass.auth.dto;

public record PasswordResetTokenResponse(
        String resetToken,
        long expiresIn
) {
}
