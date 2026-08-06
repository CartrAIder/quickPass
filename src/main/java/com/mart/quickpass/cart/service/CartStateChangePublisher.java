package com.mart.quickpass.cart.service;

import com.mart.quickpass.cart.event.CartChangeType;
import com.mart.quickpass.cart.event.CartChangedEvent;
import com.mart.quickpass.cart.repository.CartVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 실제 카트 상태가 변경된 후 버전 증가와 이벤트 발행을 같은 순서로 수행한다.
 * 이 메서드는 상태 변경이 성공한 뒤에만 호출해야 한다.
 */
@Component
@RequiredArgsConstructor
public class CartStateChangePublisher {

    private final CartVersionRepository cartVersionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public long publish(Long userId, String qrCode, CartChangeType changeType) {
        long version = cartVersionRepository.increment(qrCode);
        eventPublisher.publishEvent(CartChangedEvent.of(userId, qrCode, changeType, version));
        return version;
    }
}
