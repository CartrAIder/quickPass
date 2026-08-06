package com.mart.quickpass.cart.service;

import com.mart.quickpass.cart.dto.CartConnectRequest;
import com.mart.quickpass.cart.dto.CartConnectResponse;
import com.mart.quickpass.cart.dto.CartSession;
import com.mart.quickpass.cart.dto.CartConnectionType;
import com.mart.quickpass.cart.entity.Cart;
import com.mart.quickpass.cart.event.CartChangeType;
import com.mart.quickpass.cart.event.CartChangedEvent;
import com.mart.quickpass.cart.repository.CartItemsRepository;
import com.mart.quickpass.cart.repository.CartClaimResult;
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

import java.util.Optional;

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
    private final CartSnapshotService cartSnapshotService;
    private final ApplicationEventPublisher eventPublisher;
    private final CartStateChangePublisher stateChangePublisher;

    // 카트 연결 메서드
    @Transactional
    public CartConnectResponse connect(Long userId, CartConnectRequest request) {
        String qrCode = request.qrCode();

        // 카트 탐색
        Cart cart = cartRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new CartNotFoundException(qrCode));

        CartClaimResult claimResult = cartSessionRepository
                .claimOrResume(qrCode, CartSession.start(userId), cartSessionProperties.ttl());
        if (claimResult == CartClaimResult.CART_CONFLICT || claimResult == CartClaimResult.USER_CONFLICT) {
            throw new CartAlreadyInUseException(qrCode);
        }

        CartChangeType changeType = claimResult == CartClaimResult.CREATED
                ? CartChangeType.INITIALIZED : CartChangeType.RESUMED;
        long version;
        if (claimResult == CartClaimResult.CREATED) {
            version = stateChangePublisher.publish(userId, qrCode, changeType);
        } else {
            version = cartVersionRepository.current(qrCode);
            eventPublisher.publishEvent(CartChangedEvent.of(userId, qrCode, changeType, version));
        }

        return response(cart, claimResult == CartClaimResult.CREATED
                ? CartConnectionType.CREATED : CartConnectionType.RESUMED, version);
    }

    @Transactional(readOnly = true)
    public Optional<CartConnectResponse> current(Long userId) {
        Optional<String> qrCode = cartSessionRepository.findQrCodeByUserId(userId);
        if (qrCode.isEmpty()) {
            return Optional.empty();
        }
        Optional<CartSession> session = cartSessionRepository.findByQrCode(qrCode.get());
        if (session.isEmpty() || !session.get().userId().equals(userId)) {
            cartSessionRepository.deleteUserCart(userId);
            return Optional.empty();
        }
        Cart cart = cartRepository.findByQrCode(qrCode.get())
                .orElseThrow(() -> new CartNotFoundException(qrCode.get()));
        var ttl = cartSessionProperties.ttl();
        cartSessionRepository.refreshSessionTtl(userId, qrCode.get(), ttl);
        return Optional.of(response(cart, CartConnectionType.RESUMED,
                cartVersionRepository.current(qrCode.get())));
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
        cartSessionRepository.deleteUserCart(userId);
        // 세션 종료를 앱에 알린다
        stateChangePublisher.publish(userId, qrCode, CartChangeType.CLOSED);
    }

    private CartConnectResponse response(Cart cart, CartConnectionType type, long version) {
        return CartConnectResponse.of(
                cart, type, cartSnapshotService.snapshot(cart.getQrCode(), version));
    }
}
