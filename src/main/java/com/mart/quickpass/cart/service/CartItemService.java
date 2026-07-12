package com.mart.quickpass.cart.service;

import com.mart.quickpass.cart.dto.CartItem;
import com.mart.quickpass.cart.repository.CartItemsRepository;
import com.mart.quickpass.cart.repository.CartSessionRepository;
import com.mart.quickpass.global.config.CartSessionProperties;
import com.mart.quickpass.global.exception.CartItemNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * 앱에서 장바구니 아이템 상태를 바꾸는 유스케이스 (개수 조절 / 개별 삭제).
 *
 * <p>두 동작 모두 요청자가 해당 카트의 현재 점유자와 일치하는지 {@link CartSessionGuard}로 먼저 확인한다.
 */
@Service
@RequiredArgsConstructor
public class CartItemService {

    private final CartSessionRepository cartSessionRepository;
    private final CartItemsRepository cartItemsRepository;
    private final CartSessionProperties cartSessionProperties;
    private final CartSessionGuard cartSessionGuard;

    /**
     * 상품 수량을 delta만큼 증감한다. 증감 후 수량이 0 이하가 되면 아이템을 완전히 제거한다.
     *
     * @return 남아있는 아이템 (제거됐으면 empty)
     */
    public Optional<CartItem> adjustQuantity(Long userId, String qrCode, String barcode, long delta) {
        cartSessionGuard.requireOwnedSession(userId, qrCode);

        CartItem current = cartItemsRepository.findItem(qrCode, barcode)
                .orElseThrow(() -> new CartItemNotFoundException(barcode));

        long newQuantity = current.quantity() + delta;

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
        return result;
    }

    public void removeItem(Long userId, String qrCode, String barcode) {
        cartSessionGuard.requireOwnedSession(userId, qrCode);

        cartItemsRepository.deleteItem(qrCode, barcode);

        refreshTtl(qrCode);
    }

    private void refreshTtl(String qrCode) {
        Duration ttl = cartSessionProperties.ttl();
        cartSessionRepository.refreshTtl(qrCode, ttl);
        cartItemsRepository.refreshTtl(qrCode, ttl);
    }
}
