package com.mart.quickpass.cart.sse;

import com.mart.quickpass.global.config.CartSessionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


// 사용자별 SSE 연결 관리(현재 로컬 메모리 사용중, 추후 Redis pub/sub 등으로 인스턴스 간 팬아웃을 ㅊ가해야함)
@Slf4j
@Service
@RequiredArgsConstructor
public class CartSseService {

    private final CartSessionProperties cartSessionProperties;

    // 사용자별 SSE 연결 저장(추후 redis로 전환 예정)
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    // 사용자의 SSE 연결 생성
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(cartSessionProperties.ttl().toMillis());

        SseEmitter previous = emitters.put(userId, emitter);
        if (previous != null) {
            // 기존 연결이 있다면 이전 연결 정리
            previous.complete();
        }

        // SSE 요청 정상 완료 시 수행
        emitter.onCompletion(() -> emitters.remove(userId, emitter));

        // 생성자 지정 시간이 지날 시 수행
        emitter.onTimeout(() -> {
            emitters.remove(userId, emitter);
            emitter.complete();
        });

        // 연결 처리 중 오류 발생 시 수행
        emitter.onError(throwable -> emitters.remove(userId, emitter));

        // 최초 연결 확인용 이벤트, 클라이언트는 이 이벤트 수신으로 "구독 완료"를 판단
        sendTo(userId, emitter, "connected", "connected");

        log.info("[Sse] 구독 시작 - userId={}, 활성 연결 수={}", userId, emitters.size());
        return emitter;
    }

    // 사용자에게 이벤트 전송
    public void send(Long userId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            log.debug("[Sse] 활성 연결 없음 - userId={}, event={} (전송 생략)", userId, eventName);
            return;
        }
        sendTo(userId, emitter, eventName, data);
    }

    /** 서버가 카트 연결을 강제로 해제할 때 사용자의 SSE 연결도 종료한다. */
    public void disconnect(Long userId) {
        SseEmitter emitter = emitters.remove(userId);
        if (emitter != null) {
            emitter.complete();
        }
    }

    // 모든 활성 연결에 주석(heartbeat)을 보내 연결 유지 및 죽은 연결 정리
    public void sendHeartbeat() {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (Exception e) {
                // 이미 끊긴 연결. 조용히 정리한다.
                emitters.remove(userId, emitter);
            }
        });
    }

    // SSE 전송
    private void sendTo(Long userId, SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            // 클라이언트가 끊었거나 emitter가 이미 완료된 상태. 정리하고 에러로 종료한다.
            log.debug("[Sse] 전송 실패로 연결 정리 - userId={}, event={}", userId, eventName);
            emitters.remove(userId, emitter);
            completeWithError(emitter, e);
        }
    }

    private void completeWithError(SseEmitter emitter, Exception cause) {
        try {
            emitter.completeWithError(cause);
        } catch (RuntimeException completionError) {
            log.debug("[Sse] 이미 종료된 연결 정리 실패", completionError);
        }
    }
}
