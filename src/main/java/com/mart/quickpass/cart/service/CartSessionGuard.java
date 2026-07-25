package com.mart.quickpass.cart.service;

import com.mart.quickpass.cart.dto.CartSession;
import com.mart.quickpass.cart.repository.CartSessionRepository;
import com.mart.quickpass.global.exception.CartAccessDeniedException;
import com.mart.quickpass.global.exception.CartSessionNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


// 요청자가 해당 카트의 점유자와 일치하는지 검증
@Component
@RequiredArgsConstructor
class CartSessionGuard {

    private final CartSessionRepository cartSessionRepository;

    CartSession requireOwnedSession(Long userId, String qrCode) {
        CartSession session = cartSessionRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new CartSessionNotFoundException(qrCode));

        // 유저 일치 여부 검증
        if (!session.userId().equals(userId)) {
            throw new CartAccessDeniedException();
        }

        return session;
    }
}
