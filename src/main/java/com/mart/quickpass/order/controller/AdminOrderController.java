package com.mart.quickpass.order.controller;

import com.mart.quickpass.order.dto.AdminOrderDetailResponse;
import com.mart.quickpass.order.dto.AdminOrderPageResponse;
import com.mart.quickpass.order.entity.OrderStatus;
import com.mart.quickpass.order.service.AdminOrderService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    // 주문 목록 검색
    @GetMapping
    public ResponseEntity<AdminOrderPageResponse> search(
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(adminOrderService.search(keyword, status, page, size));
    }

    // 주문 상세 조회
    @GetMapping("/{orderId}")
    public ResponseEntity<AdminOrderDetailResponse> findByOrderId(@PathVariable String orderId) {
        return ResponseEntity.ok(adminOrderService.findByOrderId(orderId));
    }
}
