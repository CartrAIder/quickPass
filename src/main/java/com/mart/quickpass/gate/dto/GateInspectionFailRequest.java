package com.mart.quickpass.gate.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mart.quickpass.gate.entity.GateFailureReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GateInspectionFailRequest(
        @JsonProperty("gate_token") @NotBlank @Size(max = 128) String gateToken,
        @JsonProperty("failure_reason") @NotNull GateFailureReason failureReason
) {
}
