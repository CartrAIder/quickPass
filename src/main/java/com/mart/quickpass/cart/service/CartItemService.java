package com.mart.quickpass.cart.service;

import com.mart.quickpass.cart.dto.CartItem;
import com.mart.quickpass.cart.event.CartChangeType;
import com.mart.quickpass.cart.event.CartChangedEvent;
import com.mart.quickpass.cart.repository.CartItemsRepository;
import com.mart.quickpass.cart.repository.CartSessionRepository;
import com.mart.quickpass.cart.repository.CartVersionRepository;
import com.mart.quickpass.global.config.CartSessionProperties;
import com.mart.quickpass.global.exception.CartItemNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

// 스캔이 아닌 유저의 요청에 따른 장바구니 변경 로직
@Service
@RequiredArgsConstructor
public class CartItemService {

    private final CartSessionRepository cartSessionRepository;
    private final CartItemsRepository cartItemsRepository;
    private final CartVersionRepository cartVersionRepository;
    private final CartSessionProperties cartSessionProperties;
    private final CartSessionGuard cartSessionGuard;
    private final ApplicationEventPublisher eventPublisher;

    // 상품 수량 증감 메서드
    public Optional<CartItem> adjustQuantity(Long userId, String qrCode, String barcode, long delta) {
        cartSessionGuard.requireOwnedSession(userId, qrCode);

        CartItem current = cartItemsRepository.findItem(qrCode, barcode)
                .orElseThrow(() -> new CartItemNotFoundException(barcode));

        long newQuantity = delta;

        Optional<CartItem> result;
        if (newQuantity <= 0) {
            cartItemsRepository.deleteItem(qrCode, barcode);
            result = Optional.empty();
        } else {
            CartItem updated = new CartItem(current.name(), current.price(), newQuantity);
            cartItemsRepository.saveItem(qrCode, barcode, updated);
            result = Optional.of(updated);
        }

        refreshTtl(qrCode);
        publishUpdated(userId, qrCode);
        return result;
    }

    // 장바구니 물건 제거 메서드
    public void removeItem(Long userId, String qrCode, String barcode) {
        cartSessionGuard.requireOwnedSession(userId, qrCode);

        cartItemsRepository.deleteItem(qrCode, barcode);

        refreshTtl(qrCode);
        publishUpdated(userId, qrCode);
    }

    // ttl 초기화 메서드
    private void refreshTtl(String qrCode) {
        Duration ttl = cartSessionProperties.ttl();
        cartSessionRepository.refreshTtl(qrCode, ttl);
        cartItemsRepository.refreshTtl(qrCode, ttl);
    }

    // 변경된 장바구니 정보 전달(sse) 메서드
    private void publishUpdated(Long userId, String qrCode) {
        long version = cartVersionRepository.increment(qrCode);
        cartVersionRepository.refreshTtl(qrCode, cartSessionProperties.ttl());
        eventPublisher.publishEvent(CartChangedEvent.of(userId, qrCode, CartChangeType.UPDATED, version));
    }
}
