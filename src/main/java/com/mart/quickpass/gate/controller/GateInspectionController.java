package com.mart.quickpass.gate.controller;

import com.mart.quickpass.gate.dto.GateInspectionCompleteRequest;
import com.mart.quickpass.gate.dto.GateInspectionFailRequest;
import com.mart.quickpass.gate.dto.GateInspectionStartRequest;
import com.mart.quickpass.gate.dto.GateInspectionStartResponse;
import com.mart.quickpass.gate.service.GateInspectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/gate/inspections")
@RequiredArgsConstructor
public class GateInspectionController {

    private final GateInspectionService gateInspectionService;

    @PostMapping
    public GateInspectionStartResponse start(@Valid @RequestBody GateInspectionStartRequest request) {
        return gateInspectionService.start(request.gateToken(), request.gateId());
    }

    @PostMapping("/complete")
    public ResponseEntity<Void> complete(@Valid @RequestBody GateInspectionCompleteRequest request) {
        gateInspectionService.complete(request.gateToken(), request.verdict());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/fail")
    public ResponseEntity<Void> fail(@Valid @RequestBody GateInspectionFailRequest request) {
        gateInspectionService.fail(request.gateToken(), request.failureReason());
        return ResponseEntity.noContent().build();
    }
}
