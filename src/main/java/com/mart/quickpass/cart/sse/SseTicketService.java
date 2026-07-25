package com.mart.quickpass.cart.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

/**
 * SSE 구독 전용 단명(short-lived)·1회용 인증 티켓을 발급/검증한다.
 *
 * <p><b>왜 티켓인가</b>: 브라우저 {@code EventSource}는 커스텀 헤더를 실을 수 없어 인증 자격 증명을 URL에
 * 실어야 한다. 이때 범용 액세스 토큰을 그대로 URL에 노출하면 각종 인프라 로그(액세스 로그, 프록시, 브라우저
 * 히스토리)에 잔존해 전체 API 계정 탈취로 이어질 수 있다. 그래서 URL에는 오직
 * <b>SSE 구독 권한만 갖는, 수십 초짜리, 한 번 쓰면 사라지는 티켓</b>만 싣는다.
 *
 * <ul>
 *   <li>발급: {@code Authorization} 헤더로 정상 인증된 요청만 자기 자신(userId)의 티켓을 받을 수 있다.</li>
 *   <li>검증: {@code GETDEL}로 원자적으로 읽고 즉시 삭제한다(1회용). 로그에 남더라도 이미 소비되어 재사용 불가.</li>
 *   <li>만료: 짧은 TTL로, 미사용 티켓도 곧 사라진다.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SseTicketService {

    private static final String KEY_PREFIX = "cart:sse:ticket:";
    private static final Duration TICKET_TTL = Duration.ofSeconds(30);
    private static final int TOKEN_BYTES = 32; // 256-bit 엔트로피

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final StringRedisTemplate redisTemplate;

    /** 발급한 티켓이 유효한 시간(초). 클라이언트에 함께 안내한다. */
    public long ttlSeconds() {
        return TICKET_TTL.toSeconds();
    }

    /**
     * 주어진 사용자에 대한 1회용 SSE 티켓을 발급한다.
     */
    public String issue(Long userId) {
        String ticket = generateTicket();
        redisTemplate.opsForValue().set(key(ticket), String.valueOf(userId), TICKET_TTL);
        return ticket;
    }

    /**
     * 티켓을 소비(검증 후 즉시 삭제)하고 소유자 userId를 반환한다. 유효하지 않거나 이미 쓰인 티켓이면 empty.
     */
    public Optional<Long> consume(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            return Optional.empty();
        }
        String userId = redisTemplate.opsForValue().getAndDelete(key(ticket));
        if (userId == null) {
            return Optional.empty();
        }
        return Optional.of(Long.valueOf(userId));
    }

    private String generateTicket() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return URL_ENCODER.encodeToString(bytes);
    }

    private String key(String ticket) {
        return KEY_PREFIX + ticket;
    }
}
