package com.mart.quickpass.cart.service;

import com.mart.quickpass.cart.dto.CartConnectRequest;
import com.mart.quickpass.cart.dto.CartConnectResponse;
import com.mart.quickpass.cart.dto.CartSession;
import com.mart.quickpass.cart.dto.CartConnectionType;
import com.mart.quickpass.cart.dto.PendingOrderResponse;
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
import com.mart.quickpass.order.entity.OrderStatus;
import com.mart.quickpass.order.repository.OrderRepository;
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
    private final OrderRepository orderRepository;

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

        // 카트의 상태를 사용중(USING)으로 수정
        cart.markInUse();

        cartItemsRepository.refreshTtl(qrCode, cartSessionProperties.ttl());
        long version = claimResult == CartClaimResult.CREATED
                ? cartVersionRepository.increment(qrCode)
                : cartVersionRepository.current(qrCode);
        cartVersionRepository.refreshTtl(qrCode, cartSessionProperties.ttl());
        CartChangeType changeType = claimResult == CartClaimResult.CREATED
                ? CartChangeType.INITIALIZED : CartChangeType.RESUMED;
        eventPublisher.publishEvent(
                CartChangedEvent.of(userId, qrCode, changeType, version));

        return response(userId, cart, claimResult == CartClaimResult.CREATED
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
        cartSessionRepository.refreshTtl(qrCode.get(), ttl);
        cartSessionRepository.refreshUserTtl(userId, ttl);
        cartItemsRepository.refreshTtl(qrCode.get(), ttl);
        cartVersionRepository.refreshTtl(qrCode.get(), ttl);
        return Optional.of(response(userId, cart, CartConnectionType.RESUMED,
                cartVersionRepository.current(qrCode.get())));
    }

    // 카트 반납 메서드
    @Transactional
    public void disconnect(Long userId, String qrCode) {
        // 유저 일치 여부 확인
        cartSessionGuard.requireShoppingSession(userId, qrCode);

        // 카트 탐색 후 redis의 정보 지우기
        Cart cart = cartRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new CartNotFoundException(qrCode));

        close(userId, cart);
    }

    /** 결제가 승인된 주문의 카트를 추가 사용자 요청 없이 종료한다. */
    @Transactional
    public void completePayment(Long userId, Cart cart) {
        close(userId, cart);
    }

    private void close(Long userId, Cart cart) {
        String qrCode = cart.getQrCode();

        cartItemsRepository.deleteAll(qrCode);
        cartSessionRepository.deleteByQrCode(qrCode);
        cartSessionRepository.deleteUserCart(userId);
        cart.markWaiting();

        // 세션 종료를 앱에 알린다
        long version = cartVersionRepository.increment(qrCode);
        cartVersionRepository.refreshTtl(qrCode, cartSessionProperties.ttl());
        eventPublisher.publishEvent(
                CartChangedEvent.of(userId, qrCode, CartChangeType.CLOSED, version));
    }

    /** 탈퇴 시 사용자 역방향 인덱스로 현재 사용 중인 카트를 찾아 자동 반납한다. */
    @Transactional
    public void disconnectAll(Long userId) {
        Optional<String> qrCode = cartSessionRepository.findQrCodeByUserId(userId);
        if (qrCode.isEmpty()) {
            return;
        }

        Optional<CartSession> session = cartSessionRepository.findByQrCode(qrCode.get());
        if (session.isEmpty() || !session.get().userId().equals(userId)) {
            // 만료 시점 차이로 역방향 인덱스만 남은 경우 안전하게 정리한다.
            cartSessionRepository.deleteUserCart(userId);
            return;
        }

        disconnect(userId, qrCode.get());
    }

    private CartConnectResponse response(Long userId, Cart cart, CartConnectionType type, long version) {
        PendingOrderResponse pendingOrder = orderRepository.findByUserIdAndCartIdAndStatus(
                        userId, cart.getId(), OrderStatus.PENDING_PAYMENT)
                .map(PendingOrderResponse::from)
                .orElse(null);
        return CartConnectResponse.of(
                cart, type, cartSnapshotService.snapshot(cart.getQrCode(), version), pendingOrder);
    }
}
