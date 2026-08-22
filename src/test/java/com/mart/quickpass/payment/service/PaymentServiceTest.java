package com.mart.quickpass.payment.service;

import com.mart.quickpass.cart.entity.Cart;
import com.mart.quickpass.cart.entity.CartStatus;
import com.mart.quickpass.cart.service.CartConnectionService;
import com.mart.quickpass.order.entity.Order;
import com.mart.quickpass.order.entity.OrderStatus;
import com.mart.quickpass.order.repository.OrderRepository;
import com.mart.quickpass.payment.dto.PaymentAttemptCreateResult;
import com.mart.quickpass.payment.dto.PaymentConfirmRequest;
import com.mart.quickpass.payment.entity.PaymentAttempt;
import com.mart.quickpass.payment.entity.PaymentStatus;
import com.mart.quickpass.payment.repository.PaymentAttemptRepository;
import com.mart.quickpass.user.entity.User;
import com.mart.quickpass.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.EnumSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final PaymentAttemptRepository paymentAttemptRepository = mock(PaymentAttemptRepository.class);
    private final TossPaymentClient tossPaymentClient = mock(TossPaymentClient.class);
    private final CartConnectionService cartConnectionService = mock(CartConnectionService.class);

    private PaymentService paymentService;
    private Order order;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                orderRepository, paymentAttemptRepository, tossPaymentClient, cartConnectionService);

        User user = User.builder()
                .email("user@example.com")
                .password("encoded")
                .name("사용자")
                .role(UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        Cart cart = Cart.builder().qrCode("cart_001").status(CartStatus.IN_USE).build();
        ReflectionTestUtils.setField(cart, "id", 10L);
        order = Order.builder()
                .orderId("order-1")
                .user(user)
                .cart(cart)
                .orderName("우유")
                .totalAmount(3000L)
                .status(OrderStatus.PENDING_PAYMENT)
                .build();
        ReflectionTestUtils.setField(order, "id", 20L);
        when(orderRepository.findByOrderIdForUpdate("order-1")).thenReturn(Optional.of(order));
    }

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"READY", "IN_PROGRESS"})
    void returnsExistingActiveAttemptWithoutSavingAnotherAttempt(PaymentStatus activeStatus) {
        PaymentAttempt existing = attempt("attempt-active", activeStatus);
        when(paymentAttemptRepository.findFirstByOrder_IdAndStatusInOrderByIdAsc(
                20L, EnumSet.of(PaymentStatus.READY, PaymentStatus.IN_PROGRESS)))
                .thenReturn(Optional.of(existing));

        PaymentAttemptCreateResult result = paymentService.createAttempt(1L, "order-1");

        assertThat(result.created()).isFalse();
        assertThat(result.response().paymentAttemptId()).isEqualTo("attempt-active");
        assertThat(result.response().status()).isEqualTo(activeStatus);
        verify(paymentAttemptRepository, never()).save(any(PaymentAttempt.class));
    }

    @Test
    void createsNewAttemptWhenOnlyFailedAttemptsExist() {
        when(paymentAttemptRepository.findFirstByOrder_IdAndStatusInOrderByIdAsc(
                20L, EnumSet.of(PaymentStatus.READY, PaymentStatus.IN_PROGRESS)))
                .thenReturn(Optional.empty());
        when(paymentAttemptRepository.save(any(PaymentAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentAttemptCreateResult result = paymentService.createAttempt(1L, "order-1");

        assertThat(result.created()).isTrue();
        assertThat(result.response().status()).isEqualTo(PaymentStatus.READY);
        assertThat(result.response().amount()).isEqualTo(3000L);
    }

    @Test
    void approvedPaymentMarksOrderPaidAndCompletesCart() {
        PaymentAttempt attempt = attempt("attempt-1", PaymentStatus.READY);
        when(paymentAttemptRepository.findByPaymentAttemptId("attempt-1"))
                .thenReturn(Optional.of(attempt));
        when(tossPaymentClient.approve("payment-key", "order-1", 3000L))
                .thenReturn(TossPaymentClient.TossPaymentApprovalResult.success(
                        new TossPaymentClient.TossPaymentConfirmResponse(
                                "payment-key", "order-1", 3000L, "DONE", "카드")));

        var response = paymentService.confirm(
                1L, new PaymentConfirmRequest("payment-key", "order-1", 3000L, "attempt-1"));

        assertThat(response.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(attempt.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(cartConnectionService).completePayment(1L, order.getCart());
    }

    @Test
    void failedApprovalKeepsOrderAndCartActiveForRetry() {
        PaymentAttempt attempt = attempt("attempt-1", PaymentStatus.READY);
        when(paymentAttemptRepository.findByPaymentAttemptId("attempt-1"))
                .thenReturn(Optional.of(attempt));
        when(tossPaymentClient.approve("payment-key", "order-1", 3000L))
                .thenReturn(TossPaymentClient.TossPaymentApprovalResult.failure(
                        "TOSS_400", "결제 승인 실패"));

        var response = paymentService.confirm(
                1L, new PaymentConfirmRequest("payment-key", "order-1", 3000L, "attempt-1"));

        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        verify(cartConnectionService, never()).completePayment(any(), any());
    }

    private PaymentAttempt attempt(String attemptId, PaymentStatus status) {
        PaymentAttempt attempt = PaymentAttempt.builder()
                .paymentAttemptId(attemptId)
                .order(order)
                .provider("TOSS_PAYMENTS")
                .requestedAmount(3000L)
                .status(status)
                .build();
        ReflectionTestUtils.setField(attempt, "id", 30L);
        return attempt;
    }
}
