package com.mart.quickpass.cart.dto;

import java.util.List;
import java.util.Map;

/**
 * 특정 시점의 장바구니 전체 상태 스냅샷. SSE로 스마트폰 앱에 전달되어 화면을 그대로 갱신하는 데 쓰인다.
 *
 * @param qrCode        장바구니가 연동된 카트의 QR 코드
 * @param version       이 스냅샷의 단조 증가 버전. 클라이언트는 이미 렌더링한 버전보다 낮은(=오래된) 이벤트를
 *                      무시해야 한다. {@code cart-init} 수신 시 기준값을 초기화한다.
 * @param items         담긴 상품 목록 (비어 있을 수 있음)
 * @param totalQuantity 전체 상품 수량 합계
 * @param totalPrice    전체 금액 합계 ({@code price * quantity}의 총합)
 */
public record CartSnapshotResponse(
        String qrCode,
        long version,
        List<CartItemResponse> items,
        long totalQuantity,
        long totalPrice
) {

    /**
     * Redis Hash에서 읽어온 {@code barcode -> CartItem} 맵으로 스냅샷을 조립한다.
     */
    public static CartSnapshotResponse of(String qrCode, long version, Map<String, CartItem> items) {
        List<CartItemResponse> itemResponses = items.entrySet().stream()
                .map(entry -> CartItemResponse.from(entry.getKey(), entry.getValue()))
                .toList();

        long totalQuantity = itemResponses.stream()
                .mapToLong(CartItemResponse::quantity)
                .sum();
        long totalPrice = itemResponses.stream()
                .mapToLong(item -> (long) item.price() * item.quantity())
                .sum();

        return new CartSnapshotResponse(qrCode, version, itemResponses, totalQuantity, totalPrice);
    }
}
