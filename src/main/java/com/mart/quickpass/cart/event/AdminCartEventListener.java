package com.mart.quickpass.cart.event;

import com.mart.quickpass.cart.service.CartAdminService;
import com.mart.quickpass.cart.sse.AdminCartSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 사용자 카트 변경을 관리자 운영 화면에도 전달한다. */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminCartEventListener {

    private final CartAdminService cartAdminService;
    private final AdminCartSseService adminCartSseService;

    @EventListener
    public void onCartChanged(CartChangedEvent event) {
        try {
            cartAdminService.getCart(event.qrCode())
                    .ifPresent(adminCartSseService::publishCartUpdated);
        } catch (RuntimeException e) {
            // 관리자 SSE 투영 실패는 원래 카트 변경 요청과 다른 실패 경계로 처리한다.
            log.warn("[AdminCartEvent] 관리자 SSE 전송 실패 - qrCode={}, version={}",
                    event.qrCode(), event.version(), e);
        }
    }
}
