package com.mart.quickpass.payment.service;

import com.mart.quickpass.cart.service.CartConnectionService;
import com.mart.quickpass.global.exception.InvalidPaymentStateException;
import com.mart.quickpass.global.exception.OrderAccessDeniedException;
import com.mart.quickpass.global.exception.OrderNotFoundException;
import com.mart.quickpass.global.exception.PaymentAttemptNotFoundException;
import com.mart.quickpass.gate.service.GateTokenService;
import com.mart.quickpass.order.entity.Order;
import com.mart.quickpass.order.entity.OrderStatus;
import com.mart.quickpass.order.repository.OrderRepository;
import com.mart.quickpass.payment.dto.PaymentAttemptCreateResponse;
import com.mart.quickpass.payment.dto.PaymentAttemptCreateResult;
import com.mart.quickpass.payment.dto.PaymentConfirmRequest;
import com.mart.quickpass.payment.dto.PaymentConfirmResponse;
import com.mart.quickpass.payment.entity.PaymentAttempt;
import com.mart.quickpass.payment.entity.PaymentStatus;
import com.mart.quickpass.payment.repository.PaymentAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String TOSS_PAYMENTS = "TOSS_PAYMENTS";
    private static final EnumSet<PaymentStatus> ACTIVE_PAYMENT_STATUSES =
            EnumSet.of(PaymentStatus.READY, PaymentStatus.IN_PROGRESS);

    private final OrderRepository orderRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final TossPaymentClient tossPaymentsClient;
    private final GateTokenService gateTokenService;
    private final CartConnectionService cartConnectionService;


    // 결제 시도 생성 메서드
    @Transactional
    public PaymentAttemptCreateResult createAttempt(Long userId, String orderId) {
        // 주문 조회 및 상태 검사
        Order order = findOwnedOrder(userId, orderId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new InvalidPaymentStateException("결제를 시작할 수 없는 주문 상태입니다.");
        }
        var activeAttempt = paymentAttemptRepository.findFirstByOrder_IdAndStatusInOrderByIdAsc(
                order.getId(), ACTIVE_PAYMENT_STATUSES);
        if (activeAttempt.isPresent()) {
            return PaymentAttemptCreateResult.existing(PaymentAttemptCreateResponse.from(activeAttempt.get()));
        }

        PaymentAttempt attempt = PaymentAttempt.builder()
                .paymentAttemptId(generatePaymentAttemptId())
                .order(order)
                .provider(TOSS_PAYMENTS)
                .requestedAmount(order.getTotalAmount())
                .status(PaymentStatus.READY)
                .build();

        return PaymentAttemptCreateResult.created(
                PaymentAttemptCreateResponse.from(paymentAttemptRepository.save(attempt)));
    }

    // 결제 승인 메서드
    @Transactional
    public PaymentConfirmResponse confirm(Long userId, PaymentConfirmRequest request) {
        Order order = findOwnedOrder(userId, request.orderId());
        PaymentAttempt attempt = paymentAttemptRepository.findByPaymentAttemptId(request.paymentAttemptId())
                .orElseThrow(() -> new PaymentAttemptNotFoundException(request.paymentAttemptId()));

        if (!attempt.getOrder().getId().equals(order.getId())) {
            throw new PaymentAttemptNotFoundException(request.paymentAttemptId());
        }

        // successUrl의 query string은 브라우저에서 온 값이므로 신뢰하지 않는다.
        // 승인 요청에는 항상 DB의 주문 ID/금액만 사용한다.
        if (!order.getOrderId().equals(request.orderId())
                || !attempt.getRequestedAmount().equals(request.amount())
                || !order.getTotalAmount().equals(request.amount())) {
            throw new InvalidPaymentStateException("주문 ID 또는 결제 금액이 서버 주문 정보와 일치하지 않습니다.");
        }
        if (attempt.getStatus() == PaymentStatus.APPROVED) {
            if (!request.paymentKey().equals(attempt.getPaymentKey())) {
                throw new InvalidPaymentStateException("이미 승인된 결제 시도입니다.");
            }
            return PaymentConfirmResponse.from(attempt, gateTokenService.issue(order.getId()));
        }
        if (attempt.getStatus() != PaymentStatus.READY) {
            throw new InvalidPaymentStateException("승인할 수 없는 결제 시도 상태입니다.");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new InvalidPaymentStateException("승인할 수 없는 주문 상태입니다.");
        }

        // 결제 상태를 진행중으로 변경
        attempt.markInProgress();
        paymentAttemptRepository.saveAndFlush(attempt);

        TossPaymentClient.TossPaymentApprovalResult result = tossPaymentsClient.approve(
                request.paymentKey(), order.getOrderId(), order.getTotalAmount());
        if (!result.isSuccess()) {
            attempt.markFailed(request.paymentKey(), null, result.failureCode(), result.failureMessage());
            return PaymentConfirmResponse.from(attempt);
        }

        // 토스 승인 API 호출
        TossPaymentClient.TossPaymentConfirmResponse payment = result.response();
        if (!request.paymentKey().equals(payment.paymentKey())
                || !order.getOrderId().equals(payment.orderId())
                || !order.getTotalAmount().equals(payment.totalAmount())
                || !"DONE".equals(payment.status())) {
            attempt.markFailed(
                    request.paymentKey(),
                    payment.status(),
                    "PAYMENT_DATA_MISMATCH",
                    "토스페이먼츠 승인 응답의 주문 정보 또는 금액이 일치하지 않습니다."
            );
            return PaymentConfirmResponse.from(attempt);
        }

        attempt.markApproved(payment.paymentKey(), payment.totalAmount(), payment.status(), payment.method());
        order.markPaid();
        cartConnectionService.completePayment(userId, order.getCart());

        return PaymentConfirmResponse.from(attempt, gateTokenService.issue(order.getId()));
    }

    private Order findOwnedOrder(Long userId, String orderId) {
        // 결제 시도 생성·승인 모두 이 잠금을 통해 직렬화한다. 서로 다른 attempt가
        // 같은 주문을 동시에 승인하는 상황을 방지한다.
        Order order = orderRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new OrderAccessDeniedException();
        }
        return order;
    }

    private String generatePaymentAttemptId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
