package com.mart.quickpass.cart.controller;

import com.mart.quickpass.cart.dto.CartConnectRequest;
import com.mart.quickpass.cart.dto.CartConnectResponse;
import com.mart.quickpass.cart.dto.CartItemAdjustRequest;
import com.mart.quickpass.cart.dto.CartItemResponse;
import com.mart.quickpass.cart.service.CartConnectionService;
import com.mart.quickpass.cart.service.CartItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartConnectionService cartConnectionService;
    private final CartItemService cartItemService;

    // 카트 QR 스캔 - 카트 점유 + 사용자 세션 등록
    @PostMapping("/connect")
    public ResponseEntity<CartConnectResponse> connect(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CartConnectRequest request) {
        return ResponseEntity.ok(cartConnectionService.connect(userId, request));
    }

    // 앱 재실행 시 현재 사용 중인 카트와 장바구니 상태 복구
    @GetMapping("/current")
    public ResponseEntity<CartConnectResponse> current(@AuthenticationPrincipal Long userId) {
        return cartConnectionService.current(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    // 상품 수량 변경. 요청 수량이 0이면 제거되고 204 반환
    @PatchMapping("/{qrCode}/items/{barcode}")
    public ResponseEntity<CartItemResponse> adjustQuantity(
            @AuthenticationPrincipal Long userId,
            @PathVariable String qrCode,
            @PathVariable String barcode,
            @Valid @RequestBody CartItemAdjustRequest request) {
        return cartItemService.adjustQuantity(userId, qrCode, barcode, request.delta())
                .map(item -> ResponseEntity.ok(CartItemResponse.from(barcode, item)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    // 상품 개별 삭제
    @DeleteMapping("/{qrCode}/items/{barcode}")
    public ResponseEntity<Void> removeItem(
            @AuthenticationPrincipal Long userId,
            @PathVariable String qrCode,
            @PathVariable String barcode) {
        cartItemService.removeItem(userId, qrCode, barcode);
        return ResponseEntity.noContent().build();
    }

    // 카트 반납(결제 x) - 아이템 전체 비우기 + 점유 해제
    @DeleteMapping("/{qrCode}")
    public ResponseEntity<Void> disconnect(
            @AuthenticationPrincipal Long userId,
            @PathVariable String qrCode) {
        cartConnectionService.disconnect(userId, qrCode);
        return ResponseEntity.noContent().build();
    }
}
