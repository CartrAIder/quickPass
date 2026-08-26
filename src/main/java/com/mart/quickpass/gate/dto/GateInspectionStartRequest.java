package com.mart.quickpass.gate.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GateInspectionStartRequest(
        @JsonProperty("gate_token") @NotBlank @Size(max = 128) String gateToken,
        @JsonProperty("gate_id") @NotBlank @Size(max = 100) String gateId
) {
}
