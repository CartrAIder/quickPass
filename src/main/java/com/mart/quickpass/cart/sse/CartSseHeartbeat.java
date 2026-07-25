package com.mart.quickpass.cart.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


// SSE 연결 유지 및 정리 스케줄러
@Component
@RequiredArgsConstructor
public class CartSseHeartbeat {

    private final CartSseService cartSseService;

    // 15초 간격으로 전체 활성 연결에 ping 전송
    @Scheduled(fixedRate = 15_000)
    public void ping() {
        cartSseService.sendHeartbeat();
    }
}
