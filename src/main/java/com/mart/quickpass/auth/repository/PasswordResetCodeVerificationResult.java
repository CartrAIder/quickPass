package com.mart.quickpass.auth.repository;

public enum PasswordResetCodeVerificationResult {
    SUCCESS,
    EXPIRED,
    INVALID,
    ATTEMPTS_EXCEEDED
}
