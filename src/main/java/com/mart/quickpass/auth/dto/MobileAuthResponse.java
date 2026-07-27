package com.mart.quickpass.auth.dto;

public record MobileAuthResponse(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn,
        String tokenType,
        String name
) {
}
