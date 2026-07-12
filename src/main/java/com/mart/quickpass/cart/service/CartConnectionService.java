package com.mart.quickpass.cart.service;

import com.mart.quickpass.cart.dto.CartConnectRequest;
import com.mart.quickpass.cart.dto.CartConnectResponse;
import com.mart.quickpass.cart.dto.CartSession;
import com.mart.quickpass.cart.entity.Cart;
import com.mart.quickpass.cart.repository.CartItemsRepository;
import com.mart.quickpass.cart.repository.CartRepository;
import com.mart.quickpass.cart.repository.CartSessionRepository;
import com.mart.quickpass.global.config.CartSessionProperties;
import com.mart.quickpass.global.exception.CartAlreadyInUseException;
import com.mart.quickpass.global.exception.CartNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 카트 점유(연결)와 반납(해제)을 담당하는 유스케이스.
 *
 * <p>점유 여부의 실제 판단 기준은 Redis 세션이다. {@link CartSessionRepository#claim}이
 * {@code SETNX}로 원자적으로 동작하므로, 동시에 같은 카트를 스캔해도 단 한 요청만 성공한다.
 * DB의 {@link Cart#getStatus()}는 조회/표시용 보조 필드로 함께 갱신한다.
 */
@Service
@RequiredArgsConstructor
public class CartConnectionService {

    private final CartRepository cartRepository;
    private final CartSessionRepository cartSessionRepository;
    private final CartItemsRepository cartItemsRepository;
    private final CartSessionProperties cartSessionProperties;
    private final CartSessionGuard cartSessionGuard;

    @Transactional
    public CartConnectResponse connect(Long userId, CartConnectRequest request) {
        String qrCode = request.qrCode();

        Cart cart = cartRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new CartNotFoundException(qrCode));

        boolean claimed = cartSessionRepository.claim(qrCode, CartSession.start(userId), cartSessionProperties.ttl());
        if (!claimed) {
            throw new CartAlreadyInUseException(qrCode);
        }

        cart.markInUse();

        return CartConnectResponse.from(cart);
    }

    /**
     * 사용자가 직접 카트를 반납한다 ("장보기 포기"). 담겨있던 아이템을 모두 비우고,
     * 점유 세션을 삭제하며, 카트 상태를 WAITING으로 되돌려 다른 사용자가 다시 점유할 수 있게 한다.
     */
    @Transactional
    public void disconnect(Long userId, String qrCode) {
        cartSessionGuard.requireOwnedSession(userId, qrCode);

        Cart cart = cartRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new CartNotFoundException(qrCode));

        cartItemsRepository.deleteAll(qrCode);
        cartSessionRepository.deleteByQrCode(qrCode);
        cart.markWaiting();
    }
}
