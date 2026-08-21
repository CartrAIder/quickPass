package com.mart.quickpass.user.service;

import com.mart.quickpass.auth.repository.RefreshTokenRepository;
import com.mart.quickpass.cart.service.CartConnectionService;
import com.mart.quickpass.cart.sse.CartSseService;
import com.mart.quickpass.global.exception.CurrentPasswordMismatchException;
import com.mart.quickpass.global.exception.UserNotFoundException;
import com.mart.quickpass.global.exception.UserWithdrawalBlockedException;
import com.mart.quickpass.order.entity.OrderStatus;
import com.mart.quickpass.order.repository.OrderRepository;
import com.mart.quickpass.payment.entity.PaymentStatus;
import com.mart.quickpass.payment.repository.PaymentAttemptRepository;
import com.mart.quickpass.user.dto.WithdrawUserRequest;
import com.mart.quickpass.user.entity.User;
import com.mart.quickpass.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserWithdrawalService {

    // 결제가 끝나지 않은 주문 존재
    private static final EnumSet<OrderStatus> BLOCKING_ORDER_STATUSES =
            EnumSet.of(OrderStatus.PENDING_PAYMENT);
    // 결제 진행, 준비, 불확정 상태
    private static final EnumSet<PaymentStatus> BLOCKING_PAYMENT_STATUSES =
            EnumSet.of(PaymentStatus.READY, PaymentStatus.IN_PROGRESS, PaymentStatus.UNKNOWN);

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PasswordEncoder passwordEncoder;
    private final CartConnectionService cartConnectionService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CartSseService cartSseService;

    // 회원 탈퇴 메서드
    @Transactional
    public void withdraw(Long userId, WithdrawUserRequest request) {
        User user = userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 비밀번호 일치 확인
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new CurrentPasswordMismatchException();
        }

        // 진행중인 주문, 결제가 있다면 탈퇴 중지
        if (orderRepository.existsByUserIdAndStatusIn(userId, BLOCKING_ORDER_STATUSES)
                || paymentAttemptRepository.existsByOrderUserIdAndStatusIn(userId, BLOCKING_PAYMENT_STATUSES)) {
            throw new UserWithdrawalBlockedException();
        }

        cartConnectionService.disconnectAll(userId);    // 카트 연결 해제
        refreshTokenRepository.deleteByUserId(userId);  // 리프레시 토큰 삭제
        cartSseService.disconnect(userId);              // sse 연결 종

        // 이메일 unique 제약을 유지하면서 재가입을 허용하고, 기존 거래의 FK는 그대로 보존
        user.withdraw(
                "withdrawn-" + userId + "-" + UUID.randomUUID() + "@anonymous.invalid",
                passwordEncoder.encode(UUID.randomUUID().toString())
        );
    }
}
