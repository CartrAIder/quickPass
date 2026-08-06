package com.mart.quickpass.cart.sse;

import com.mart.quickpass.cart.dto.AdminCartResponse;
import com.mart.quickpass.global.config.CartSessionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 관리자용 카트 운영 화면의 SSE 연결과 전체 브로드캐스트를 관리한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCartSseService {

    private final CartSessionProperties cartSessionProperties;
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter connect() {
        String emitterId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(cartSessionProperties.ttl().toMillis());
        emitters.put(emitterId, emitter);
        emitter.onCompletion(() -> emitters.remove(emitterId, emitter));
        emitter.onTimeout(() -> {
            emitters.remove(emitterId, emitter);
            emitter.complete();
        });
        emitter.onError(throwable -> emitters.remove(emitterId, emitter));
        send(emitterId, emitter, "connected", "connected");
        return emitter;
    }

    public void publishCartUpdated(AdminCartResponse cart) {
        emitters.forEach((emitterId, emitter) -> send(emitterId, emitter, "cart-updated", cart));
    }

    private void send(String emitterId, SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            log.debug("[AdminCartSse] 전송 실패로 연결 정리 - event={}", eventName);
            emitters.remove(emitterId, emitter);
            completeWithError(emitter, e);
        }
    }

    private void completeWithError(SseEmitter emitter, Exception cause) {
        try {
            emitter.completeWithError(cause);
        } catch (RuntimeException completionError) {
            log.debug("[AdminCartSse] 이미 종료된 연결 정리 실패", completionError);
        }
    }
}
