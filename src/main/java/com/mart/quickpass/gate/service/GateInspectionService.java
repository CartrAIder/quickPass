package com.mart.quickpass.gate.service;

import com.mart.quickpass.gate.dto.GateInspectionStartResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GateInspectionService {

    private final GateTokenRepository gateTokenRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;

    @Transactional(readOnly = true)
    public GateInspectionStartResponse start(String token, String gateId) {
        GateTokenSnapshot snapshot = findToken(token);
        validatePreClaimState(snapshot, gateId);

        Order order = orderRepository.findById(snapshot.orderId())
                .orElseThrow(() -> new GateException(ErrorCode.GATE_ORDER_NOT_FOUND, "주문을 찾을 수 없습니다."));
        if (order.getStatus() != OrderStatus.PAID
                || !paymentAttemptRepository.existsByOrder_IdAndStatus(order.getId(), PaymentStatus.APPROVED)) {
            throw new GateException(ErrorCode.GATE_PAYMENT_INVALID, "유효한 결제 완료 주문이 아닙니다.");
        }

        List<GateInspectionStartResponse.Item> items = orderItemRepository.findGateItemsByOrderId(order.getId()).stream()
                .map(this::toItem)
                .toList();
        GateTransitionResult claim = gateTokenRepository.claim(token, gateId);
        if (claim == GateTransitionResult.NOT_FOUND) {
            throw tokenNotFound();
        }
        if (claim == GateTransitionResult.CONFLICT) {
            throw new GateException(ErrorCode.GATE_STATE_CONFLICT, "Gate Token을 선점할 수 없습니다.");
        }
        return new GateInspectionStartResponse(items);
    }

    public void complete(String token, GateVerdict verdict) {
        assertTransitionSucceeded(gateTokenRepository.complete(token, verdict.name()), "완료");
    }

    public void fail(String token, GateFailureReason reason) {
        assertTransitionSucceeded(gateTokenRepository.fail(token, reason.name()), "실패");
    }

    private GateTokenSnapshot findToken(String token) {
        return gateTokenRepository.find(token).orElseThrow(this::tokenNotFound);
    }

    private void validatePreClaimState(GateTokenSnapshot snapshot, String gateId) {
        if (snapshot.state() == GateTokenState.AVAILABLE) {
            return;
        }
        if (snapshot.state() == GateTokenState.IN_PROGRESS && gateId.equals(snapshot.gateId())) {
            return;
        }
        ErrorCode code = switch (snapshot.state()) {
            case USED -> ErrorCode.GATE_TOKEN_USED;
            case REVOKED -> ErrorCode.GATE_TOKEN_REVOKED;
            case IN_PROGRESS -> ErrorCode.GATE_IN_PROGRESS_AT_OTHER_GATE;
            default -> ErrorCode.GATE_STATE_CONFLICT;
        };
        throw new GateException(code, "검사를 시작할 수 없는 Gate Token 상태입니다.");
    }

    private void assertTransitionSucceeded(GateTransitionResult result, String action) {
        if (result == GateTransitionResult.SUCCESS || result == GateTransitionResult.IDEMPOTENT) {
            return;
        }
        if (result == GateTransitionResult.NOT_FOUND) {
            throw tokenNotFound();
        }
        throw new GateException(ErrorCode.GATE_STATE_CONFLICT, "Gate 검사 " + action + " 상태 전이가 충돌했습니다.");
    }

    private GateException tokenNotFound() {
        return new GateException(ErrorCode.GATE_TOKEN_NOT_FOUND, "Gate Token이 없거나 만료되었습니다.");
    }

    private GateInspectionStartResponse.Item toItem(GateItemRow row) {
        return new GateInspectionStartResponse.Item(row.barcode(), row.qty());
    }
}
