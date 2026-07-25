package com.mart.quickpass.cart.dto;

/**
 * SSE 구독용 단명 티켓 발급 응답.
 *
 * @param ticket           SSE 구독 시 {@code /api/carts/subscribe?ticket=...}로 전달할 1회용 티켓
 * @param expiresInSeconds 티켓 유효 시간(초). 이 시간 내에 SSE 연결을 열어야 한다.
 */
public record SseTicketResponse(
        String ticket,
        long expiresInSeconds
) {
}
