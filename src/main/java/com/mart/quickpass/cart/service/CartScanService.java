package com.mart.quickpass.cart.service;

import com.mart.quickpass.cart.dto.CartItem;
import com.mart.quickpass.cart.dto.CartScanMessage;
import com.mart.quickpass.cart.repository.CartItemsRepository;
import com.mart.quickpass.cart.repository.CartRepository;
import com.mart.quickpass.cart.repository.CartSessionRepository;
import com.mart.quickpass.global.config.CartSessionProperties;
import com.mart.quickpass.product.entity.Product;
import com.mart.quickpass.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 카트 스캔 이벤트의 비즈니스 로직 진입점.
 *
 * <p>MQTT 프로토콜과 무관한 순수 도메인 계층이다. {@code global.mqtt}의 Subscriber가
 * MQTT 메시지를 파싱해 이 메서드를 호출한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartScanService {

    private static final long SCAN_QUANTITY_DELTA = 1;

    private final CartRepository cartRepository;
    private final CartSessionRepository cartSessionRepository;
    private final CartItemsRepository cartItemsRepository;
    private final ProductRepository productRepository;
    private final CartSessionProperties cartSessionProperties;

    /**
     * 특정 카트에서 발생한 바코드 스캔을 처리한다.
     *
     * @param qrCode 스캔이 발생한 카트의 QR 코드 (MQTT 토픽에서 추출)
     * @param scan   스캔 payload
     */
    public void handleScan(String qrCode, CartScanMessage scan) {
        if (cartRepository.findByQrCode(qrCode).isEmpty()) {
            // 등록되지 않은 카트에서 온 메시지는 무시한다 (오작동/장난 발행 방어)
            log.warn("[CartScan] 등록되지 않은 카트 - qrCode={}", qrCode);
            return;
        }

        if (cartSessionRepository.findByQrCode(qrCode).isEmpty()) {
            // 앱과 연동(QR 연결)되지 않은 카트의 스캔은 반영할 사용자가 없으므로 무시한다
            log.warn("[CartScan] 연동되지 않은 카트의 스캔 - qrCode={}", qrCode);
            return;
        }

        Product product = productRepository.findByBarcode(scan.barcode()).orElse(null);
        if (product == null) {
            log.warn("[CartScan] 등록되지 않은 상품 바코드 - qrCode={}, barcode={}", qrCode, scan.barcode());
            return;
        }

        addOrIncrementItem(qrCode, product);

        // 활동이 있었으므로 점유 세션과 아이템 세션의 유휴 타임아웃을 함께 초기화한다 (sliding TTL)
        var ttl = cartSessionProperties.ttl();
        cartSessionRepository.refreshTtl(qrCode, ttl);
        cartItemsRepository.refreshTtl(qrCode, ttl);

        log.info("[CartScan] qrCode={}, barcode={}, scannedAt={}", qrCode, scan.barcode(), scan.scannedAt());
    }

    private void addOrIncrementItem(String qrCode, Product product) {
        CartItem item = cartItemsRepository.findItem(qrCode, product.getBarcode())
                .map(existing -> existing.incrementQuantity(SCAN_QUANTITY_DELTA))
                .orElseGet(() -> new CartItem(product.getName(), product.getPrice(), SCAN_QUANTITY_DELTA));

        cartItemsRepository.saveItem(qrCode, product.getBarcode(), item);
    }
}
