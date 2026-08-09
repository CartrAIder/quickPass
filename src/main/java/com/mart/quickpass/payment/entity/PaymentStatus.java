package com.mart.quickpass.payment.entity;

public enum PaymentStatus {

    READY, // 결제 시도 생성(초기)

    IN_PROGRESS, // 결제 승인 요청 진행중

    APPROVED, // 결제 승인

    FAILED, // 결제 승인 실패

    CANCELED, // 사용자 취소, 결제 절차 중단

    UNKNOWN // 결제사 상태 내부 매핑 실패(예외 상태)
}