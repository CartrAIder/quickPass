package com.mart.quickpass.user.service;

import com.mart.quickpass.auth.repository.RefreshTokenRepository;
import com.mart.quickpass.cart.service.CartConnectionService;
import com.mart.quickpass.cart.sse.CartSseService;
import com.mart.quickpass.order.repository.OrderRepository;
import com.mart.quickpass.payment.repository.PaymentAttemptRepository;
import com.mart.quickpass.user.dto.WithdrawUserRequest;
import com.mart.quickpass.user.entity.User;
import com.mart.quickpass.user.entity.UserRole;
import com.mart.quickpass.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserWithdrawalServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final PaymentAttemptRepository paymentAttemptRepository = mock(PaymentAttemptRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final CartConnectionService cartConnectionService = mock(CartConnectionService.class);
    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final CartSseService cartSseService = mock(CartSseService.class);
    private final UserWithdrawalService service = new UserWithdrawalService(
            userRepository,
            orderRepository,
            paymentAttemptRepository,
            passwordEncoder,
            cartConnectionService,
            refreshTokenRepository,
            cartSseService);

    @Test
    void withdrawalLocksUserBeforeDisconnectingCurrentCart() {
        User user = User.builder()
                .email("user@example.com")
                .password("encoded-password")
                .name("사용자")
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current-password", "encoded-password")).thenReturn(true);
        when(passwordEncoder.encode(any())).thenReturn("withdrawn-password");

        service.withdraw(1L, new WithdrawUserRequest("current-password"));

        assertThat(user.isActive()).isFalse();
        verify(cartConnectionService).disconnectAll(1L);
    }
}
