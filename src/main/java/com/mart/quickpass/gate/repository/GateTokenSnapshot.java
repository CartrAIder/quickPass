package com.mart.quickpass.gate.repository;

import com.mart.quickpass.gate.entity.GateTokenState;

public record GateTokenSnapshot(
        Long orderId,
        GateTokenState state,
        String gateId,
        String verdict,
        String failureReason
) {
}
