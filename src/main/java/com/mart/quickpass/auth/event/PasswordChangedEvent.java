package com.mart.quickpass.auth.event;

public record PasswordChangedEvent(Long userId, String refreshToken) {
}
