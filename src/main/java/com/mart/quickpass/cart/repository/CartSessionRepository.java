package com.mart.quickpass.cart.repository;

import com.mart.quickpass.cart.dto.CartSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

/**
 * qrCode 기준으로 카트 점유 세션(String)을 Redis에 보관하는 리포지토리.
 */
@Repository
@RequiredArgsConstructor
public class CartSessionRepository {

    private static final String KEY_PREFIX = "cart:session:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 세션이 존재하지 않을 때만 저장한다 (Redis {@code SETNX} 기반 원자적 점유).
     *
     * @return 점유에 성공했으면 true, 이미 다른 세션이 있으면 false
     */
    public boolean claim(String qrCode, CartSession session, Duration ttl) {
        Boolean claimed = redisTemplate.opsForValue()
                .setIfAbsent(key(qrCode), objectMapper.writeValueAsString(session), ttl);
        return Boolean.TRUE.equals(claimed);
    }

    public Optional<CartSession> findByQrCode(String qrCode) {
        String value = redisTemplate.opsForValue().get(key(qrCode));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.readValue(value, CartSession.class));
    }

    public void deleteByQrCode(String qrCode) {
        redisTemplate.delete(key(qrCode));
    }

    /** 유휴 타임아웃을 초기화한다 (sliding TTL). 세션이 없으면 아무 효과가 없다. */
    public void refreshTtl(String qrCode, Duration ttl) {
        redisTemplate.expire(key(qrCode), ttl);
    }

    private String key(String qrCode) {
        return KEY_PREFIX + qrCode;
    }
}
