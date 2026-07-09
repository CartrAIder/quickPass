package com.mart.quickpass.cart.service;

import com.mart.quickpass.cart.dto.CartScanMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 카트 스캔 이벤트의 비즈니스 로직 진입점.
 *
 * <p>MQTT 프로토콜과 무관한 순수 도메인 계층이다. {@code global.mqtt}의 Subscriber가
 * MQTT 메시지를 파싱해 이 메서드를 호출한다.
 *
 * <p>현재는 뼈대(로그만)이며, 다음 작업이 여기에 채워진다:
 * <ol>
 *   <li>Redis 세션에서 cartId → 사용 중인 user 조회 (미연동 카트면 무시)</li>
 *   <li>바코드로 Product 조회 후 장바구니 상태 갱신</li>
 *   <li>해당 user의 SSE 스트림으로 갱신 내역 push</li>
 * </ol>
 */
@Slf4j
@Service
public class CartScanService {

    /**
     * 특정 카트에서 발생한 바코드 스캔을 처리한다.
     *
     * @param cartId 스캔이 발생한 카트 ID (MQTT 토픽에서 추출)
     * @param scan   스캔 payload
     */
    public void handleScan(Long cartId, CartScanMessage scan) {
        // TODO: Redis 세션 조회 → Product 조회 → 장바구니 갱신 → SSE push
        log.info("[CartScan] cartId={}, barcode={}, scannedAt={}",
                cartId, scan.barcode(), scan.scannedAt());
    }
}
