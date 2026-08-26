package com.mart.quickpass.gate.service;

import com.mart.quickpass.gate.dto.GateItemRow;
import com.mart.quickpass.gate.entity.GateFailureReason;
import com.mart.quickpass.gate.entity.GateTokenState;
import com.mart.quickpass.gate.entity.GateVerdict;
import com.mart.quickpass.gate.exception.GateException;
import com.mart.quickpass.gate.repository.GateTokenRepository;
import com.mart.quickpass.gate.repository.GateTokenSnapshot;
import com.mart.quickpass.gate.repository.GateTransitionResult;
import com.mart.quickpass.global.exception.ErrorCode;
import com.mart.quickpass.order.entity.Order;
import com.mart.quickpass.order.entity.OrderStatus;
import com.mart.quickpass.order.repository.OrderItemRepository;
import com.mart.quickpass.order.repository.OrderRepository;
import com.mart.quickpass.payment.entity.PaymentStatus;
import com.mart.quickpass.payment.repository.PaymentAttemptRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GateInspectionServiceTest {

    private final GateTokenRepository tokens = mock(GateTokenRepository.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final OrderItemRepository items = mock(OrderItemRepository.class);
    private final PaymentAttemptRepository payments = mock(PaymentAttemptRepository.class);
    private final GateInspectionService service = new GateInspectionService(tokens, orders, items, payments);

    @Test
    void availableTokenIsClaimedAndOnlyBarcodeAndQuantityAreReturned() {
        Order order = paidOrder(7L);
        when(tokens.find("token")).thenReturn(Optional.of(snapshot(GateTokenState.AVAILABLE, null)));
        when(orders.findById(7L)).thenReturn(Optional.of(order));
        when(payments.existsByOrder_IdAndStatus(7L, PaymentStatus.APPROVED)).thenReturn(true);
        when(items.findGateItemsByOrderId(7L)).thenReturn(List.of(new GateItemRow("8801", 2)));
        when(tokens.claim("token", "GATE-03")).thenReturn(GateTransitionResult.SUCCESS);

        var response = service.start("token", "GATE-03");

        assertThat(response.items()).containsExactly(new com.mart.quickpass.gate.dto.GateInspectionStartResponse.Item("8801", 2));
    }

    @Test
    void sameGateRetryIsIdempotent() {
        Order order = paidOrder(7L);
        when(tokens.find("token")).thenReturn(Optional.of(snapshot(GateTokenState.IN_PROGRESS, "GATE-03")));
        when(orders.findById(7L)).thenReturn(Optional.of(order));
        when(payments.existsByOrder_IdAndStatus(7L, PaymentStatus.APPROVED)).thenReturn(true);
        when(items.findGateItemsByOrderId(7L)).thenReturn(List.of());
        when(tokens.claim("token", "GATE-03")).thenReturn(GateTransitionResult.IDEMPOTENT);

        assertThat(service.start("token", "GATE-03").items()).isEmpty();
    }

    @Test
    void otherGateCannotClaimInProgressToken() {
        when(tokens.find("token")).thenReturn(Optional.of(snapshot(GateTokenState.IN_PROGRESS, "GATE-01")));

        assertThatThrownBy(() -> service.start("token", "GATE-03"))
                .isInstanceOfSatisfying(GateException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.GATE_IN_PROGRESS_AT_OTHER_GATE));
    }

    @Test
    void completeAndFailAcceptIdempotentRedisResults() {
        when(tokens.complete("token", "PASS")).thenReturn(GateTransitionResult.IDEMPOTENT);
        when(tokens.fail("token", "AI_TIMEOUT")).thenReturn(GateTransitionResult.IDEMPOTENT);

        service.complete("token", GateVerdict.PASS);
        service.fail("token", GateFailureReason.AI_TIMEOUT);

        verify(tokens).complete("token", "PASS");
        verify(tokens).fail("token", "AI_TIMEOUT");
    }

    @Test
    void conflictingCompletionIsRejected() {
        when(tokens.complete("token", "FLAG")).thenReturn(GateTransitionResult.CONFLICT);

        assertThatThrownBy(() -> service.complete("token", GateVerdict.FLAG))
                .isInstanceOfSatisfying(GateException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.GATE_STATE_CONFLICT));
    }

    private GateTokenSnapshot snapshot(GateTokenState state, String gateId) {
        return new GateTokenSnapshot(7L, state, gateId, null, null);
    }

    private Order paidOrder(Long id) {
        Order order = mock(Order.class);
        when(order.getId()).thenReturn(id);
        when(order.getStatus()).thenReturn(OrderStatus.PAID);
        return order;
    }
}
