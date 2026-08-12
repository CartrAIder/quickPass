package com.mart.quickpass.cart.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/** QoS 1 재전송으로 같은 스캔이 중복 반영되는 것을 막는다. */
@Repository
@RequiredArgsConstructor
public class CartScanDeduplicationRepository {

    private static final String KEY_PREFIX = "cart:scan:dedup:";

    private final StringRedisTemplate redisTemplate;

    /**
     * scanId를 처음 처리하는 경우에만 true를 반환한다.
     * Redis SET NX와 TTL을 함께 사용하므로 여러 서버 인스턴스에서도 원자적으로 동작한다.
     */
    public boolean tryMarkProcessed(String qrCode, String scanId, Duration ttl) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue()
                .setIfAbsent(key(qrCode, scanId), "true", ttl));
    }

    private String key(String qrCode, String scanId) {
        return KEY_PREFIX + qrCode + ":" + scanId;
    }
}
