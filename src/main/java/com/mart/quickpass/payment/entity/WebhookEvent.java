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
    private Long id; // 기본키

    @Column(name = "transmission_id", nullable = false, length = 200)
    private String transmissionId; // 고유 ID

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType; // 이벤트 종류

    @Column(name = "payment_key", length = 200)
    private String paymentKey; // 결제 식별키

    @Column(name = "external_order_id", length = 64)
    private String externalOrderId; // 주문 번호

    @Column(nullable = false, columnDefinition = "json")
    private String payload; // 토스 원본 json 저장

    @Enumerated(EnumType.STRING)
    @Column(name = "process_status", nullable = false, length = 30)
    private WebhookProcessStatus processStatus; // 웹훅 처리 상태

    @Column(name = "retry_count", nullable = false)
    private int retryCount; // 재시도 횟수

    @Column(name = "failure_message", length = 500)
    private String failureMessage; // 실패 메시지

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt; // 서버 도착 시간

    @Column(name = "processed_at")
    private LocalDateTime processedAt; // 처리 종료 시간

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 웹훅 생성 시간

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 갱신 시간

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
