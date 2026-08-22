package com.mart.quickpass.cart.service;

import com.mart.quickpass.cart.dto.CartSession;
import com.mart.quickpass.cart.repository.CartSessionRepository;
import com.mart.quickpass.cart.repository.CartRepository;
import com.mart.quickpass.global.exception.CartAccessDeniedException;
import com.mart.quickpass.global.exception.CartSessionNotFoundException;
import com.mart.quickpass.global.exception.CartPaymentPendingException;
import com.mart.quickpass.global.exception.CartNotFoundException;
import com.mart.quickpass.order.entity.OrderStatus;
import com.mart.quickpass.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


// 요청자가 해당 카트의 점유자와 일치하는지 검증
@Component
@RequiredArgsConstructor
class CartSessionGuard {

    private final CartSessionRepository cartSessionRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;

    CartSession requireOwnedSession(Long userId, String qrCode) {
        CartSession session = cartSessionRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new CartSessionNotFoundException(qrCode));

        // 유저 일치 여부 검증
        if (!session.userId().equals(userId)) {
            throw new CartAccessDeniedException();
        }

        return session;
    }

    CartSession requireShoppingSession(Long userId, String qrCode) {
        // 주문 생성과 같은 카트 행 잠금을 사용해 활성 주문 확인부터 Redis 변경까지 직렬화한다.
        cartRepository.findByQrCodeForUpdate(qrCode)
                .orElseThrow(() -> new CartNotFoundException(qrCode));
        CartSession session = requireOwnedSession(userId, qrCode);
        if (orderRepository.existsByCartQrCodeAndStatus(qrCode, OrderStatus.PENDING_PAYMENT)) {
            throw new CartPaymentPendingException();
        }
        return session;
    }
}
