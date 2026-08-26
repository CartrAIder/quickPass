package com.mart.quickpass.gate.repository;

import com.mart.quickpass.gate.entity.GateTokenState;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class GateTokenRepository {

    private static final String TOKEN_PREFIX = "gate:token:";
    private static final String ORDER_PREFIX = "gate:order:";

    private static final DefaultRedisScript<String> ISSUE_SCRIPT = new DefaultRedisScript<>("""
            local existing = redis.call('GET', KEYS[1])
            if existing then return existing end
            if redis.call('EXISTS', KEYS[2]) == 1 then return '__TOKEN_COLLISION__' end
            redis.call('HSET', KEYS[2], 'orderId', ARGV[1], 'state', 'AVAILABLE')
            redis.call('SET', KEYS[1], ARGV[2])
            redis.call('PEXPIREAT', KEYS[1], ARGV[3])
            redis.call('PEXPIREAT', KEYS[2], ARGV[3])
            return ARGV[2]
            """, String.class);

    private static final DefaultRedisScript<String> CLAIM_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[1]) == 0 then return 'NOT_FOUND' end
            local state = redis.call('HGET', KEYS[1], 'state')
            if state == 'AVAILABLE' then
                redis.call('HSET', KEYS[1], 'state', 'IN_PROGRESS', 'gateId', ARGV[1])
                return 'SUCCESS'
            end
            if state == 'IN_PROGRESS' and redis.call('HGET', KEYS[1], 'gateId') == ARGV[1] then
                return 'IDEMPOTENT'
            end
            return 'CONFLICT'
            """, String.class);

    private static final DefaultRedisScript<String> COMPLETE_SCRIPT = transitionScript("USED", "verdict");
    private static final DefaultRedisScript<String> FAIL_SCRIPT = transitionScript("FAILED", "failureReason");
    private static final DefaultRedisScript<String> REVOKE_SCRIPT = new DefaultRedisScript<>("""
            local token = redis.call('GET', KEYS[1])
            if not token then return 'NOT_FOUND' end
            local tokenKey = 'gate:token:' .. token
            local state = redis.call('HGET', tokenKey, 'state')
            if state == 'AVAILABLE' or state == 'IN_PROGRESS' then
                redis.call('HSET', tokenKey, 'state', 'REVOKED')
                return 'SUCCESS'
            end
            if state == 'REVOKED' then return 'IDEMPOTENT' end
            return 'CONFLICT'
            """, String.class);

    private final StringRedisTemplate redisTemplate;

    public String issue(Long orderId, String candidateToken, Instant expiresAt) {
        return redisTemplate.execute(ISSUE_SCRIPT,
                List.of(orderKey(orderId), tokenKey(candidateToken)),
                orderId.toString(), candidateToken, Long.toString(expiresAt.toEpochMilli()));
    }

    public Optional<GateTokenSnapshot> find(String token) {
        Map<Object, Object> values = redisTemplate.opsForHash().entries(tokenKey(token));
        if (values.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new GateTokenSnapshot(
                    Long.valueOf((String) values.get("orderId")),
                    GateTokenState.valueOf((String) values.get("state")),
                    (String) values.get("gateId"),
                    (String) values.get("verdict"),
                    (String) values.get("failureReason")));
        } catch (RuntimeException e) {
            throw new IllegalStateException("Gate Token 데이터가 올바르지 않습니다.", e);
        }
    }

    public GateTransitionResult claim(String token, String gateId) {
        return execute(CLAIM_SCRIPT, tokenKey(token), gateId);
    }

    public GateTransitionResult complete(String token, String verdict) {
        return execute(COMPLETE_SCRIPT, tokenKey(token), verdict);
    }

    public GateTransitionResult fail(String token, String failureReason) {
        return execute(FAIL_SCRIPT, tokenKey(token), failureReason);
    }

    public GateTransitionResult revoke(Long orderId) {
        return execute(REVOKE_SCRIPT, orderKey(orderId), "");
    }

    private GateTransitionResult execute(DefaultRedisScript<String> script, String key, String value) {
        String result = redisTemplate.execute(script, List.of(key), value);
        if (result == null) {
            throw new IllegalStateException("Gate Token 상태 변경 결과가 없습니다.");
        }
        return GateTransitionResult.valueOf(result);
    }

    private static DefaultRedisScript<String> transitionScript(String targetState, String field) {
        return new DefaultRedisScript<>("""
                if redis.call('EXISTS', KEYS[1]) == 0 then return 'NOT_FOUND' end
                local state = redis.call('HGET', KEYS[1], 'state')
                if state == 'IN_PROGRESS' then
                    redis.call('HSET', KEYS[1], 'state', '%s', '%s', ARGV[1])
                    return 'SUCCESS'
                end
                if state == '%s' and redis.call('HGET', KEYS[1], '%s') == ARGV[1] then
                    return 'IDEMPOTENT'
                end
                return 'CONFLICT'
                """.formatted(targetState, field, targetState, field), String.class);
    }

    private String tokenKey(String token) {
        return TOKEN_PREFIX + token;
    }

    private String orderKey(Long orderId) {
        return ORDER_PREFIX + orderId;
    }
}
