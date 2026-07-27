package com.mart.quickpass.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 토스페이먼츠가 전송한 웹훅 원문과 처리 상태를 보관한다.
 * transmissionId의 유니크 제약조건으로 동일 웹훅의 중복 수신을 방지한다.
 */
@Entity
@Table(
        name = "webhook_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_webhook_events_transmission_id",
                columnNames = "transmission_id"
        )
)
@Check(name = "ck_webhook_events_retry_count", constraints = "retry_count >= 0")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transmission_id", nullable = false, length = 200)
    private String transmissionId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "payment_key", length = 200)
    private String paymentKey;

    @Column(name = "external_order_id", length = 64)
    private String externalOrderId;

    @Column(nullable = false, columnDefinition = "json")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "process_status", nullable = false, length = 30)
    private WebhookProcessStatus processStatus;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public WebhookEvent(
            String transmissionId,
            String eventType,
            String paymentKey,
            String externalOrderId,
            String payload,
            WebhookProcessStatus processStatus,
            int retryCount,
            String failureMessage,
            LocalDateTime receivedAt,
            LocalDateTime processedAt
    ) {
        this.transmissionId = transmissionId;
        this.eventType = eventType;
        this.paymentKey = paymentKey;
        this.externalOrderId = externalOrderId;
        this.payload = payload;
        this.processStatus = processStatus;
        this.retryCount = retryCount;
        this.failureMessage = failureMessage;
        this.receivedAt = receivedAt;
        this.processedAt = processedAt;
    }
}
