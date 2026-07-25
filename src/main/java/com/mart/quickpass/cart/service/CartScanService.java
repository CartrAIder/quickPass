package com.mart.quickpass.cart.service;

import com.mart.quickpass.cart.dto.CartItem;
import com.mart.quickpass.cart.dto.CartScanMessage;
import com.mart.quickpass.cart.dto.CartSession;
import com.mart.quickpass.cart.event.CartChangeType;
import com.mart.quickpass.cart.event.CartChangedEvent;
import com.mart.quickpass.cart.repository.CartItemsRepository;
import com.mart.quickpass.cart.repository.CartRepository;
import com.mart.quickpass.cart.repository.CartSessionRepository;
import com.mart.quickpass.cart.repository.CartVersionRepository;
import com.mart.quickpass.global.config.CartSessionProperties;
import com.mart.quickpass.product.entity.Product;
import com.mart.quickpass.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Optional;


// 바코드 스캔을 처리한다
@Slf4j
@Service
@RequiredArgsConstructor
public class CartScanService {

    private static final long SCAN_QUANTITY_DELTA = 1;

    private final CartRepository cartRepository;
    private final CartSessionRepository cartSessionRepository;
    private final CartItemsRepository cartItemsRepository;
    private final CartVersionRepository cartVersionRepository;
    private final ProductRepository productRepository;
    private final CartSessionProperties cartSessionProperties;
    private final ApplicationEventPublisher eventPublisher;

    // 카트에서 발생한 바코드 스캔을 처리한다
    public void handleScan(String qrCode, CartScanMessage scan) {
        if (cartRepository.findByQrCode(qrCode).isEmpty()) {
            // 등록되지 않은 카트에서 온 메시지는 무시한다 (오작동/장난 발행 방어)
            log.warn("[CartScan] 등록되지 않은 카트 - qrCode={}", qrCode);
            return;
        }

        Optional<CartSession> session = cartSessionRepository.findByQrCode(qrCode);
        if (session.isEmpty()) {
            // 앱과 연동(QR 연결)되지 않은 카트의 스캔은 반영할 사용자가 없으므로 무시한다
            log.warn("[CartScan] 연동되지 않은 카트의 스캔 - qrCode={}", qrCode);
            return;
        }

        Product product = productRepository.findByBarcode(scan.barcode()).orElse(null);
        if (product == null) {
            // 존재하지 않는 삼품의 스캔은 무시한다
            log.warn("[CartScan] 등록되지 않은 상품 바코드 - qrCode={}, barcode={}", qrCode, scan.barcode());
            return;
        }

        addOrIncrementItem(qrCode, product);

        // 점유 세션, 아이템 세션의 ttl을 초기화
        var ttl = cartSessionProperties.ttl();
        cartSessionRepository.refreshTtl(qrCode, ttl);
        cartItemsRepository.refreshTtl(qrCode, ttl);

        // 장바구니 ttl 초기화 + 사용자의 스마트폰으로 데이터 전송(SSE)
        long version = cartVersionRepository.increment(qrCode);
        cartVersionRepository.refreshTtl(qrCode, ttl);
        eventPublisher.publishEvent(
                CartChangedEvent.of(session.get().userId(), qrCode, CartChangeType.UPDATED, version));

        log.info("[CartScan] qrCode={}, barcode={}, scannedAt={}", qrCode, scan.barcode(), scan.scannedAt());
    }


    // 장바구니 상태 변경 메서드
    private void addOrIncrementItem(String qrCode, Product product) {
        // 이미 있다면 수량 증가(+ 1), 없다면 새 항목 생성
        CartItem item = cartItemsRepository.findItem(qrCode, product.getBarcode())
                .map(existing -> existing.incrementQuantity(SCAN_QUANTITY_DELTA))
                .orElseGet(() -> new CartItem(product.getName(), product.getPrice(), SCAN_QUANTITY_DELTA));

        cartItemsRepository.saveItem(qrCode, product.getBarcode(), item);
    }
}
