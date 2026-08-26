package com.mart.quickpass.gate.repository;

public enum GateTransitionResult {
    SUCCESS,
    IDEMPOTENT,
    NOT_FOUND,
    CONFLICT
}
