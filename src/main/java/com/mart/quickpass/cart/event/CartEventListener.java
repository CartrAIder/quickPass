package com.mart.quickpass.cart.event;

import com.mart.quickpass.cart.dto.CartSnapshotResponse;
import com.mart.quickpass.cart.service.CartSnapshotService;
import com.mart.quickpass.cart.sse.CartSseService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;


// 이벤트 처리 리스너
@Component
@RequiredArgsConstructor
public class CartEventListener {

    private final CartSnapshotService cartSnapshotService;
    private final CartSseService cartSseService;

    @EventListener
    public void onCartChanged(CartChangedEvent event) {
        // 스냅샷 생성 및 반환
        CartSnapshotResponse snapshot = cartSnapshotService.snapshot(event.qrCode(), event.version());
        cartSseService.send(event.userId(), event.type().eventName(), snapshot);
    }
}
