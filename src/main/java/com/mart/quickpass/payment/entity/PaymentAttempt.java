package com.mart.quickpass.payment.entity;

import com.mart.quickpass.order.entity.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "payment_attempts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_attempts_attempt_id", columnNames = "payment_attempt_id"),
                @UniqueConstraint(name = "uk_payment_attempts_payment_key", columnNames = "payment_key")
        }
)
@Check(name = "ck_payment_attempts_requested_amount", constraints = "requested_amount > 0")
@Check(
        name = "ck_payment_attempts_approved_amount",
        constraints = "approved_amount IS NULL OR approved_amount > 0"
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 기본키

    @Column(name = "payment_attempt_id", nullable = false, length = 64)
    private String paymentAttemptId; // 결제 시도 체크 번호

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "order_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_payment_attempts_order")
    )
    private Order order; // 주문

    @Column(name = "payment_key", length = 200)
    private String paymentKey; // 결제 시도 키(토스 페이먼츠 측)

    @Column(nullable = false, length = 30)
    private String provider; // 결제 제공자(추후 확장성 용도)

    @Column(length = 30)
    private String method; // 결제 수단

    @Column(name = "requested_amount", nullable = false)
    private Long requestedAmount; // 요청 금액

    @Column(name = "approved_amount")
    private Long approvedAmount; // 승인 금액

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status; // 결제 상태

    @Column(name = "provider_status", length = 30)
    private String providerStatus; // 결제사 결제 상태

    @Column(name = "failure_code", length = 100)
    private String failureCode; // 결제 실패 코드

    @Column(name = "failure_message", length = 500)
    private String failureMessage; // 결제 실패 메시지

    @Column(name = "requested_at")
    private LocalDateTime requestedAt; // 요청 시각

    @Column(name = "approved_at")
    private LocalDateTime approvedAt; // 승인 시각

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성 시간

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 수정 시간

    @Version
    @Column(nullable = false)
    private Long version; // 버전(낙관적 락)

    @Builder
    public PaymentAttempt(
            String paymentAttemptId,
            Order order,
            String paymentKey,
            String provider,
            String method,
            Long requestedAmount,
            Long approvedAmount,
            PaymentStatus status,
            String providerStatus,
            String failureCode,
            String failureMessage,
            LocalDateTime requestedAt,
            LocalDateTime approvedAt
    ) {
        this.paymentAttemptId = paymentAttemptId;
        this.order = order;
        this.paymentKey = paymentKey;
        this.provider = provider;
        this.method = method;
        this.requestedAmount = requestedAmount;
        this.approvedAmount = approvedAmount;
        this.status = status;
        this.providerStatus = providerStatus;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
        this.requestedAt = requestedAt;
        this.approvedAt = approvedAt;
    }


    //상태 변화 메서드//

    public void markInProgress() {
        this.status = PaymentStatus.IN_PROGRESS;
        this.requestedAt = LocalDateTime.now();
    }


    public void markApproved(String paymentKey, Long approvedAmount, String providerStatus, String method) {
        this.paymentKey = paymentKey;
        this.approvedAmount = approvedAmount;
        this.providerStatus = providerStatus;
        this.method = method;
        this.status = PaymentStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
        this.failureCode = null;
        this.failureMessage = null;
    }

    public void markFailed(String paymentKey, String providerStatus, String failureCode, String failureMessage) {
        this.paymentKey = paymentKey;
        this.providerStatus = providerStatus;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
        this.status = PaymentStatus.FAILED;
    }
}
