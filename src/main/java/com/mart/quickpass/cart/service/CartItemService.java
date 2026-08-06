package com.mart.quickpass.cart.service;

import com.mart.quickpass.cart.dto.CartItem;
import com.mart.quickpass.cart.event.CartChangeType;
import com.mart.quickpass.cart.repository.CartItemsRepository;
import com.mart.quickpass.cart.repository.CartSessionRepository;
import com.mart.quickpass.global.config.CartSessionProperties;
import com.mart.quickpass.global.exception.CartItemNotFoundException;
import com.mart.quickpass.global.exception.InvalidCartItemQuantityException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

// 스캔이 아닌 유저의 요청에 따른 장바구니 변경 로직
@Service
@RequiredArgsConstructor
public class CartItemService {

    private final CartSessionRepository cartSessionRepository;
    private final CartItemsRepository cartItemsRepository;
    private final CartSessionProperties cartSessionProperties;
    private final CartSessionGuard cartSessionGuard;
    private final CartStateChangePublisher stateChangePublisher;

    // 상품 수량 변경 메서드 (delta를 최종 수량으로 사용)
    public Optional<CartItem> adjustQuantity(Long userId, String qrCode, String barcode, long delta) {
        if (delta < 0) {
            throw new InvalidCartItemQuantityException(delta);
        }

        cartSessionGuard.requireOwnedSession(userId, qrCode);

        CartItem current = cartItemsRepository.findItem(qrCode, barcode)
                .orElseThrow(() -> new CartItemNotFoundException(barcode));

        if (delta == current.quantity()) {
            refreshTtl(userId, qrCode);
            return Optional.of(current);
        }

        Optional<CartItem> result;
        if (delta == 0) {
            cartItemsRepository.deleteItem(qrCode, barcode);
            result = Optional.empty();
        } else {
            CartItem updated = new CartItem(current.name(), current.price(), delta);
            cartItemsRepository.saveItem(qrCode, barcode, updated);
            result = Optional.of(updated);
        }

        refreshTtl(userId, qrCode);
        stateChangePublisher.publish(userId, qrCode, CartChangeType.UPDATED);
        return result;
    }

    // 장바구니 물건 제거 메서드
    public void removeItem(Long userId, String qrCode, String barcode) {
        cartSessionGuard.requireOwnedSession(userId, qrCode);

        cartItemsRepository.findItem(qrCode, barcode)
                .orElseThrow(() -> new CartItemNotFoundException(barcode));

        cartItemsRepository.deleteItem(qrCode, barcode);

        refreshTtl(userId, qrCode);
        stateChangePublisher.publish(userId, qrCode, CartChangeType.UPDATED);
    }

    // 점유 세션, 사용자 역인덱스, 아이템 세션의 TTL 초기화
    private void refreshTtl(Long userId, String qrCode) {
        cartSessionRepository.refreshSessionTtl(userId, qrCode, cartSessionProperties.ttl());
    }

}
