package com.mart.quickpass.gate.dto;

import java.util.List;

public record GateInspectionStartResponse(List<Item> items) {
    public record Item(String barcode, Integer qty) {
    }
}
