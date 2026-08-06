package com.mart.quickpass.cart.service;

import com.mart.quickpass.cart.dto.AdminCartResponse;
import com.mart.quickpass.cart.dto.AdminCartDetailResponse;
import com.mart.quickpass.cart.dto.AdminCartStatus;
import com.mart.quickpass.cart.dto.AdminCartUserResponse;
import com.mart.quickpass.cart.dto.CartSession;
import com.mart.quickpass.cart.dto.CartSnapshotResponse;
import com.mart.quickpass.cart.entity.Cart;
import com.mart.quickpass.cart.entity.CartStatus;
import com.mart.quickpass.cart.repository.CartRepository;
import com.mart.quickpass.cart.repository.CartItemsRepository;
import com.mart.quickpass.cart.repository.CartSessionRepository;
import com.mart.quickpass.cart.repository.CartVersionRepository;
import com.mart.quickpass.cart.event.CartChangeType;
import com.mart.quickpass.cart.sse.CartSseService;
import com.mart.quickpass.global.exception.CartNotFoundException;
import com.mart.quickpass.user.entity.User;
import com.mart.quickpass.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartAdminService {

    private final CartRepository cartRepository;
    private final CartItemsRepository cartItemsRepository;
    private final CartSessionRepository cartSessionRepository;
    private final CartVersionRepository cartVersionRepository;
    private final CartSnapshotService cartSnapshotService;
    private final UserRepository userRepository;
    private final CartSseService cartSseService;
    private final CartStateChangePublisher stateChangePublisher;

    public java.util.List<AdminCartResponse> getCartList() {
        java.util.List<Cart> carts = cartRepository.findAll();
        Map<String, CartSession> sessions = carts.stream()
                .map(Cart::getQrCode)
                .map(qrCode -> Map.entry(qrCode, cartSessionRepository.findByQrCode(qrCode)))
                .filter(entry -> entry.getValue().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));
        Map<Long, User> users = findUsers(sessions.values());

        return carts.stream()
                .map(cart -> toResponse(cart, sessions.get(cart.getQrCode()), users))
                .toList();
    }

    public Optional<AdminCartResponse> getCart(String qrCode) {
        return cartRepository.findByQrCode(qrCode)
                .map(cart -> {
                    CartSession session = cartSessionRepository.findByQrCode(qrCode).orElse(null);
                    Map<Long, User> users = session == null ? Map.of() : findUsers(java.util.List.of(session));
                    return toResponse(cart, session, users);
                });
    }

    public AdminCartDetailResponse getCartDetail(Long cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new CartNotFoundException(String.valueOf(cartId)));
        CartSession session = cartSessionRepository.findByQrCode(cart.getQrCode()).orElse(null);
        Map<Long, User> users = session == null ? Map.of() : findUsers(java.util.List.of(session));
        CartSnapshotResponse snapshot = cartSnapshotService.snapshot(
                cart.getQrCode(), cartVersionRepository.current(cart.getQrCode()));
        User user = session == null ? null : users.get(session.userId());

        return new AdminCartDetailResponse(
                cart.getId(),
                cart.getQrCode(),
                snapshot.version(),
                toAdminStatus(cart.getStatus(), session),
                user == null ? null : AdminCartUserResponse.from(user),
                session == null ? null : session.startedAt(),
                snapshot.items(),
                snapshot.totalQuantity(),
                snapshot.totalPrice());
    }

    @Transactional
    public void forceDisconnect(Long cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new CartNotFoundException(String.valueOf(cartId)));
        CartSession session = cartSessionRepository.findByQrCode(cart.getQrCode()).orElse(null);
        Long userId = session == null ? null : session.userId();

        cartItemsRepository.deleteAll(cart.getQrCode());
        cartSessionRepository.deleteByQrCode(cart.getQrCode());
        if (userId != null) {
            cartSessionRepository.deleteUserCart(userId);
        }
        stateChangePublisher.publish(userId, cart.getQrCode(), CartChangeType.CLOSED);
        if (userId != null) {
            cartSseService.disconnect(userId);
        }
    }

    private Map<Long, User> findUsers(Collection<CartSession> sessions) {
        return userRepository.findAllById(sessions.stream().map(CartSession::userId).toList()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private AdminCartResponse toResponse(Cart cart, CartSession session, Map<Long, User> users) {
        CartSnapshotResponse snapshot = cartSnapshotService.snapshot(
                cart.getQrCode(), cartVersionRepository.current(cart.getQrCode()));
        User user = session == null ? null : users.get(session.userId());

        return new AdminCartResponse(
                cart.getId(),
                cart.getQrCode(),
                snapshot.version(),
                toAdminStatus(cart.getStatus(), session),
                user == null ? null : AdminCartUserResponse.from(user),
                snapshot.totalQuantity(),
                snapshot.totalPrice());
    }

    private AdminCartStatus toAdminStatus(CartStatus status, CartSession session) {
        if (status == CartStatus.DISABLED) {
            return AdminCartStatus.DISABLED;
        }
        return session == null ? AdminCartStatus.AVAILABLE : AdminCartStatus.IN_USE;
    }
}
