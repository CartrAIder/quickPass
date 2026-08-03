package com.mart.quickpass.payment.entity;

public enum WebhookProcessStatus {

    RECEIVED, // 웹훅 수신 완료
    PROCESSING, // 웹훅 처리 중
    PROCESSED, // 웹훅 처리 정상 완료
    FAILED // 처리 중 오류 발생
}