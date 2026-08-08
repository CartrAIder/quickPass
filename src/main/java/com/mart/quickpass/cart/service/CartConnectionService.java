package com.mart.quickpass.cart.service;

import com.mart.quickpass.cart.dto.CartConnectRequest;
import com.mart.quickpass.cart.dto.CartConnectResponse;
import com.mart.quickpass.cart.dto.CartSession;
import com.mart.quickpass.cart.entity.Cart;
import com.mart.quickpass.cart.event.CartChangeType;
import com.mart.quickpass.cart.event.CartChangedEvent;
import com.mart.quickpass.cart.repository.CartItemsRepository;
import com.mart.quickpass.cart.repository.CartRepository;
import com.mart.quickpass.cart.repository.CartSessionRepository;
import com.mart.quickpass.cart.repository.CartVersionRepository;
import com.mart.quickpass.global.config.CartSessionProperties;
import com.mart.quickpass.global.exception.CartAlreadyInUseException;
import com.mart.quickpass.global.exception.CartNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

// 카트 연결 & 해제 담당 서비스 로직
@Service
@RequiredArgsConstructor
public class CartConnectionService {

    private final CartRepository cartRepository;
    private final CartSessionRepository cartSessionRepository;
    private final CartItemsRepository cartItemsRepository;
    private final CartVersionRepository cartVersionRepository;
    private final CartSessionProperties cartSessionProperties;
    private final CartSessionGuard cartSessionGuard;
    private final ApplicationEventPublisher eventPublisher;

    // 카트 연결 메서드
    @Transactional
    public CartConnectResponse connect(Long userId, CartConnectRequest request) {
        String qrCode = request.qrCode();

        // 카트 탐색
        Cart cart = cartRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new CartNotFoundException(qrCode));

        // 카트 세션(redis) 생성
        boolean claimed = cartSessionRepository.claim(qrCode, CartSession.start(userId), cartSessionProperties.ttl());
        if (!claimed) {
            throw new CartAlreadyInUseException(qrCode);
        }

        // 카트의 상태를 사용중(USING)으로 수정
        cart.markInUse();

        // 연동 직후 빈 장바구니를 앱으로 밀어 화면을 동기화한다.
        long version = cartVersionRepository.increment(qrCode);
        cartVersionRepository.refreshTtl(qrCode, cartSessionProperties.ttl());
        eventPublisher.publishEvent(
                CartChangedEvent.of(userId, qrCode, CartChangeType.INITIALIZED, version));

        return CartConnectResponse.from(cart);
    }

    // 카트 반납 메서드
    @Transactional
    public void disconnect(Long userId, String qrCode) {
        // 유저 일치 여부 확인
        cartSessionGuard.requireOwnedSession(userId, qrCode);

        // 카트 탐색 후 redis의 정보 지우기
        Cart cart = cartRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new CartNotFoundException(qrCode));

        cartItemsRepository.deleteAll(qrCode);
        cartSessionRepository.deleteByQrCode(qrCode);
        cart.markWaiting();

        // 세션 종료를 앱에 알린다
        long version = cartVersionRepository.increment(qrCode);
        cartVersionRepository.refreshTtl(qrCode, cartSessionProperties.ttl());
        eventPublisher.publishEvent(
                CartChangedEvent.of(userId, qrCode, CartChangeType.CLOSED, version));
    }

    /** 탈퇴 시 이 사용자가 점유한 모든 카트를 정상 반납 상태로 돌린다. */
    @Transactional
    public void disconnectAll(Long userId) {
        Set<String> qrCodes = Set.copyOf(cartSessionRepository.findQrCodesByUserId(userId));
        qrCodes.forEach(qrCode -> disconnect(userId, qrCode));
    }
}
