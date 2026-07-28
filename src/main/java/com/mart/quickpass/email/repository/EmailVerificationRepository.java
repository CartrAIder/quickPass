package com.mart.quickpass.email.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EmailVerificationRepository {

    private static final String CODE_KEY_PREFIX = "emailVerification:code:";
    private static final String VERIFIED_KEY_PREFIX = "emailVerification:verified:";
    private static final String COOLDOWN_KEY_PREFIX = "emailVerification:cooldown:";

    private final StringRedisTemplate redisTemplate;

    public void saveCode(String email, String codeHash, Duration ttl) {
        redisTemplate.opsForValue().set(codeKey(email), codeHash, ttl);
    }

    public Optional<String> findCodeHash(String email) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(codeKey(email)));
    }

    public void deleteCode(String email) {
        redisTemplate.delete(codeKey(email));
    }

    public void markVerified(String email, Duration ttl) {
        redisTemplate.opsForValue().set(verifiedKey(email), "true", ttl);
    }

    public boolean isVerified(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(verifiedKey(email)));
    }

    public void consumeVerified(String email) {
        redisTemplate.delete(verifiedKey(email));
    }

    /**
     * GET 후 SET으로 확인하면 동시에 들어온 발송 요청이 모두 통과할 수 있다.
     * Redis SET NX로 재발송 제한 획득을 원자적으로 처리한다.
     */
    public boolean acquireCooldown(String email, Duration ttl) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue()
                .setIfAbsent(cooldownKey(email), "true", ttl));
    }

    public void deleteCooldown(String email) {
        redisTemplate.delete(cooldownKey(email));
    }

    private String codeKey(String email) {
        return CODE_KEY_PREFIX + email;
    }

    private String verifiedKey(String email) {
        return VERIFIED_KEY_PREFIX + email;
    }

    private String cooldownKey(String email) {
        return COOLDOWN_KEY_PREFIX + email;
    }
}
