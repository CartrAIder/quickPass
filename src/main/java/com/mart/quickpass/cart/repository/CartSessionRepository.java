package com.mart.quickpass.cart.repository;

import com.mart.quickpass.cart.dto.CartSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

// redis 카트 세션 레포지토리
@Repository
@RequiredArgsConstructor
public class CartSessionRepository {

    private static final String KEY_PREFIX = "cart:session:";
    private static final String USER_KEY_PREFIX = "cart:user:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // 세션이 없는 경우에만 저장(이미 있으면 false 반환)
    public boolean claim(String qrCode, CartSession session, Duration ttl) {
        Boolean claimed = redisTemplate.opsForValue()
                .setIfAbsent(key(qrCode), objectMapper.writeValueAsString(session), ttl);
        if (Boolean.TRUE.equals(claimed)) {
            redisTemplate.opsForSet().add(userKey(session.userId()), qrCode);
            redisTemplate.expire(userKey(session.userId()), ttl);
            return true;
        }
        return false;
    }

    public Optional<CartSession> findByQrCode(String qrCode) {
        String value = redisTemplate.opsForValue().get(key(qrCode));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.readValue(value, CartSession.class));
    }

    public void deleteByQrCode(String qrCode) {
        findByQrCode(qrCode).ifPresent(session ->
                redisTemplate.opsForSet().remove(userKey(session.userId()), qrCode));
        redisTemplate.delete(key(qrCode));
    }

    public Set<String> findQrCodesByUserId(Long userId) {
        Set<String> qrCodes = redisTemplate.opsForSet().members(userKey(userId));
        return qrCodes == null ? Set.of() : qrCodes;
    }

    // TTL 초기화
    public void refreshTtl(String qrCode, Duration ttl) {
        redisTemplate.expire(key(qrCode), ttl);
        findByQrCode(qrCode).ifPresent(session -> redisTemplate.expire(userKey(session.userId()), ttl));
    }

    private String key(String qrCode) {
        return KEY_PREFIX + qrCode;
    }

    private String userKey(Long userId) {
        return USER_KEY_PREFIX + userId + ":carts";
    }
}
