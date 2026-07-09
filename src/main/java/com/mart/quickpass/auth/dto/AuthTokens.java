package com.mart.quickpass.auth.dto;

public record AuthTokens(
        String accessToken,
        String refreshToken
) {
}
