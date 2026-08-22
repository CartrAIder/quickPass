package com.mart.quickpass.auth.repository;

import com.mart.quickpass.global.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private static final String KEY_PREFIX = "refreshToken:";

    // 저장된 현재 토큰과 요청 토큰이 일치할 때만 원자적으로 삭제
    private static final DefaultRedisScript<Long> DELETE_IF_MATCHES_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    // 저장된 현재 토큰이 요청 토큰과 일치할 때만 새 토큰과 TTL로 원자적으로 교체
    private static final DefaultRedisScript<Long> ROTATE_IF_MATCHES_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
                return 1
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;

    // 리프레시 토큰 저장 (userId 기준, 만료 기간만큼 TTL 적용)
    public void save(Long userId, String refreshToken) {
        redisTemplate.opsForValue().set(
                key(userId),
                refreshToken,
                Duration.ofMillis(jwtProperties.refreshTokenValidity())
        );
    }

    // 사용자 탈퇴 또는 비밀번호 재설정 후 기존 로그인 세션을 조건 없이 폐기한다.
    public void deleteByUserId(Long userId) {
        redisTemplate.delete(key(userId));
    }

    // 요청 토큰이 현재 저장된 토큰과 일치할 때만 원자적으로 삭제한다.
    public boolean deleteIfMatches(Long userId, String refreshToken) {
        Long deleted = redisTemplate.execute(
                DELETE_IF_MATCHES_SCRIPT,
                List.of(key(userId)),
                refreshToken
        );
        return Long.valueOf(1L).equals(deleted);
    }

    // 현재 토큰이 요청 토큰과 일치할 때만 새 토큰과 TTL로 원자적으로 교체한다.
    public boolean rotateIfMatches(Long userId, String currentToken, String newToken) {
        Long rotated = redisTemplate.execute(
                ROTATE_IF_MATCHES_SCRIPT,
                List.of(key(userId)),
                currentToken,
                newToken,
                String.valueOf(jwtProperties.refreshTokenValidity())
        );
        return Long.valueOf(1L).equals(rotated);
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
