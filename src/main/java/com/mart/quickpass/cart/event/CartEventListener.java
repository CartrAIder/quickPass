package com.mart.quickpass.cart.event;

import com.mart.quickpass.cart.dto.CartSnapshotResponse;
import com.mart.quickpass.cart.service.CartSnapshotService;
import com.mart.quickpass.cart.sse.CartSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;


// 이벤트 처리 리스너
@Component
@RequiredArgsConstructor
@Slf4j
public class CartEventListener {

    private final CartSnapshotService cartSnapshotService;
    private final CartSseService cartSseService;

    @EventListener
    public void onCartChanged(CartChangedEvent event) {
        if (event.userId() == null) {
            return;
        }
        try {
            CartSnapshotResponse snapshot = cartSnapshotService.snapshot(event.qrCode(), event.version());
            cartSseService.send(event.userId(), event.type().eventName(), snapshot);
        } catch (RuntimeException e) {
            // SSE 투영 실패는 이미 성공한 카트 변경 요청을 실패로 바꾸지 않는다.
            log.warn("[CartEvent] 사용자 SSE 전송 실패 - userId={}, qrCode={}, version={}",
                    event.userId(), event.qrCode(), event.version(), e);
        }
    }
}
