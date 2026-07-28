package com.mart.quickpass.email.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EmailVerificationRepository {

    // Redis 키 접두사
    private static final String CODE_KEY_PREFIX = "emailVerification:code:";
    private static final String VERIFIED_KEY_PREFIX = "emailVerification:verified:";
    private static final String COOLDOWN_KEY_PREFIX = "emailVerification:cooldown:";

    private final StringRedisTemplate redisTemplate;

    // 인증번호의 SHA-256 해시를 Redis에 저장
    public void saveCode(String email, String codeHash, Duration ttl) {
        redisTemplate.opsForValue().set(codeKey(email), codeHash, ttl);
    }

    // 인증 해시번호 조회
    public Optional<String> findCodeHash(String email) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(codeKey(email)));
    }

    // 인증번호 삭제
    public void deleteCode(String email) {
        redisTemplate.delete(codeKey(email));
    }


    // 인증 완료상태 저장
    public void markVerified(String email, Duration ttl) {
        redisTemplate.opsForValue().set(verifiedKey(email), "true", ttl);
    }

    // 인증 완료 여부 확인
    public boolean isVerified(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(verifiedKey(email)));
    }

    // 인증 완료 상태 삭제
    public void consumeVerified(String email) {
        redisTemplate.delete(verifiedKey(email));
    }

    // 재발송 제한 cooldown키 저장(원자적 처리)
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
