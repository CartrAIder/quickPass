package com.mart.quickpass.cart.controller;

import com.mart.quickpass.cart.dto.AdminCartResponse;
import com.mart.quickpass.cart.dto.AdminCartDetailResponse;
import com.mart.quickpass.cart.service.CartAdminService;
import com.mart.quickpass.cart.sse.AdminCartSseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/admin/carts")
@RequiredArgsConstructor
public class AdminCartController {

    private final CartAdminService cartAdminService;
    private final AdminCartSseService adminCartSseService;

    // 카트 목록 조회
    @GetMapping
    public ResponseEntity<List<AdminCartResponse>> getCartList() {
        return ResponseEntity.ok(cartAdminService.getCartList());
    }

    // 카트 상세 정보 조회
    @GetMapping("/{cartId}")
    public ResponseEntity<AdminCartDetailResponse> getCartDetail(@PathVariable Long cartId) {
        return ResponseEntity.ok(cartAdminService.getCartDetail(cartId));
    }

    // 카트 연결 강제 해제
    @PostMapping("/{cartId}/disconnect")
    public ResponseEntity<Void> forceDisconnect(@PathVariable Long cartId) {
        cartAdminService.forceDisconnect(cartId);
        return ResponseEntity.noContent().build();
    }

    // 관리자 sse 연결
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        return adminCartSseService.connect();
    }
}
