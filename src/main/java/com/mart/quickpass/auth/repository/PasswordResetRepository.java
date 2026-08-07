package com.mart.quickpass.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PasswordResetRepository {

    private static final String CODE_KEY_PREFIX = "passwordReset:code:";
    private static final String COOLDOWN_KEY_PREFIX = "passwordReset:cooldown:";
    private static final String TOKEN_KEY_PREFIX = "passwordReset:token:";

    private static final DefaultRedisScript<Long> CONSUME_CODE_SCRIPT = new DefaultRedisScript<>("""
            local stored = redis.call('GET', KEYS[1])
            if not stored or stored ~= ARGV[1] then
                return 0
            end
            redis.call('DEL', KEYS[1])
            return 1
            """, Long.class);

    private static final DefaultRedisScript<String> CONSUME_TOKEN_SCRIPT = new DefaultRedisScript<>("""
            local email = redis.call('GET', KEYS[1])
            if not email then
                return nil
            end
            redis.call('DEL', KEYS[1])
            return email
            """, String.class);

    private final StringRedisTemplate redisTemplate;

    public boolean acquireCooldown(String email, Duration ttl) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue()
                .setIfAbsent(cooldownKey(email), "true", ttl));
    }

    public void deleteCooldown(String email) {
        redisTemplate.delete(cooldownKey(email));
    }

    public void saveCode(String email, String codeHash, Duration ttl) {
        redisTemplate.opsForValue().set(codeKey(email), codeHash, ttl);
    }

    public Optional<String> findCodeHash(String email) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(codeKey(email)));
    }

    public boolean consumeCode(String email, String codeHash) {
        Long result = redisTemplate.execute(CONSUME_CODE_SCRIPT, List.of(codeKey(email)), codeHash);
        return Long.valueOf(1L).equals(result);
    }

    public void saveToken(String tokenHash, String email, Duration ttl) {
        redisTemplate.opsForValue().set(tokenKey(tokenHash), email, ttl);
    }

    public Optional<String> findEmailByToken(String tokenHash) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(tokenKey(tokenHash)));
    }

    public Optional<String> consumeToken(String tokenHash) {
        return Optional.ofNullable(redisTemplate.execute(
                CONSUME_TOKEN_SCRIPT,
                List.of(tokenKey(tokenHash))
        ));
    }

    private String codeKey(String email) {
        return CODE_KEY_PREFIX + email;
    }

    private String cooldownKey(String email) {
        return COOLDOWN_KEY_PREFIX + email;
    }

    private String tokenKey(String tokenHash) {
        return TOKEN_KEY_PREFIX + tokenHash;
    }
}
