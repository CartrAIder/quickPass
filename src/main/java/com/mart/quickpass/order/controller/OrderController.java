package com.mart.quickpass.order.controller;

import com.mart.quickpass.order.dto.OrderCreateRequest;
import com.mart.quickpass.order.dto.OrderCreateResponse;
import com.mart.quickpass.order.dto.OrderCreateResult;
import com.mart.quickpass.order.dto.OrderDetailResponse;
import com.mart.quickpass.order.dto.OrderHistoryResponse;
import com.mart.quickpass.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 주문 생성 컨트롤러
    @PostMapping
    public ResponseEntity<OrderCreateResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody OrderCreateRequest request
    ) {
        OrderCreateResult result = orderService.create(userId, request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.response());
    }

    @PostMapping("/{orderId}/abandon")
    public ResponseEntity<Void> abandon(
            @AuthenticationPrincipal Long userId,
            @PathVariable String orderId
    ) {
        orderService.abandon(userId, orderId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<OrderHistoryResponse> findMyPurchaseHistory(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(orderService.findMyPurchaseHistory(userId, page, size));
    }

    @GetMapping("/me/{orderId}")
    public ResponseEntity<OrderDetailResponse> findMyPurchaseDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable String orderId
    ) {
        return ResponseEntity.ok(orderService.findMyPurchaseDetail(userId, orderId));
    }
}
