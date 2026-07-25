package com.mart.quickpass.cart.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;


// 장바구니 버전 관리 레포지토리
@Repository
@RequiredArgsConstructor
public class CartVersionRepository {

    private static final String KEY_PREFIX = "cart:version:";

    private final StringRedisTemplate redisTemplate;

    // 버전 갱신(증가)
    public long increment(String qrCode) {
        Long value = redisTemplate.opsForValue().increment(key(qrCode));
        return value == null ? 0L : value;
    }

    // TTL 초기화
    public void refreshTtl(String qrCode, Duration ttl) {
        redisTemplate.expire(key(qrCode), ttl);
    }

    private String key(String qrCode) {
        return KEY_PREFIX + qrCode;
    }
}
