package com.mart.quickpass.auth.event;

public record PasswordResetTransactionEvent(Long userId, String email, String tokenHash) {
}
