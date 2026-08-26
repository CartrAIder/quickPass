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
    private static final String ATTEMPTS_KEY_PREFIX = "passwordReset:attempts:";
    private static final String COOLDOWN_KEY_PREFIX = "passwordReset:cooldown:";
    private static final String TOKEN_KEY_PREFIX = "passwordReset:token:";
    private static final String TOKEN_LOCK_KEY_PREFIX = "passwordReset:tokenLock:";
    private static final int MAX_CONFIRM_ATTEMPTS = 5;

    // 새 인증 코드를 저장하면서 이전 코드의 실패 횟수를 함께 초기화
    private static final DefaultRedisScript<Long> SAVE_CODE_SCRIPT = new DefaultRedisScript<>("""
            redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2])
            redis.call('DEL', KEYS[2])
            return 1
            """, Long.class);

    // 코드 조회·비교와 실패 횟수 증가를 원자적으로 처리해 동시 요청에도 최대 시도 횟수를 보장
    // 반환값: 0=만료, 1=성공, 2=불일치, 3=최대 시도 횟수 초과
    private static final DefaultRedisScript<Long> VERIFY_CODE_SCRIPT = new DefaultRedisScript<>("""
            local stored = redis.call('GET', KEYS[1])
            if not stored then
                return 0
            end
            if stored == ARGV[1] then
                redis.call('DEL', KEYS[1], KEYS[2])
                return 1
            end
            local attempts = redis.call('INCR', KEYS[2])
            if attempts == 1 then
                local ttl = redis.call('PTTL', KEYS[1])
                if ttl > 0 then
                    redis.call('PEXPIRE', KEYS[2], ttl)
                end
            end
            if attempts >= tonumber(ARGV[2]) then
                redis.call('DEL', KEYS[1], KEYS[2], KEYS[3])
                return 3
            end
            return 2
            """, Long.class);

    // 재설정 토큰이 유효할 때만 같은 TTL의 잠금 키를 생성해 한 요청에 사용권을 부여한다.
    private static final DefaultRedisScript<String> ACQUIRE_TOKEN_SCRIPT = new DefaultRedisScript<>("""
            local email = redis.call('GET', KEYS[1])
            if not email then
                return nil
            end
            local ttl = redis.call('PTTL', KEYS[1])
            if ttl <= 0 then
                return nil
            end
            if not redis.call('SET', KEYS[2], email, 'PX', ttl, 'NX') then
                return nil
            end
            return email
            """, String.class);

    // 비밀번호 변경 트랜잭션이 커밋된 뒤 재설정 토큰과 잠금을 함께 삭제한다.
    private static final DefaultRedisScript<Long> COMPLETE_TOKEN_SCRIPT = new DefaultRedisScript<>("""
            if not redis.call('GET', KEYS[2]) then
                return 0
            end
            redis.call('DEL', KEYS[1], KEYS[2])
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    // 이메일별 발송 제한 키가 없을 때만 TTL과 함께 생성한다.
    public boolean acquireCooldown(String email, Duration ttl) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue()
                .setIfAbsent(cooldownKey(email), "true", ttl));
    }

    // 이메일 발송 또는 코드 저장 실패 시 즉시 재요청할 수 있도록 발송 제한을 해제한다.
    public void deleteCooldown(String email) {
        redisTemplate.delete(cooldownKey(email));
    }

    // 원문 인증 코드 대신 해시를 저장하고 이전 실패 횟수를 초기화한다.
    public void saveCode(String email, String codeHash, Duration ttl) {
        redisTemplate.execute(
                SAVE_CODE_SCRIPT,
                List.of(codeKey(email), attemptsKey(email)),
                codeHash,
                String.valueOf(ttl.toMillis())
        );
    }

    // 인증 코드 검증 결과를 Lua 스크립트의 숫자 반환값에서 비즈니스 상태 Enum으로 변환한다.
    public PasswordResetCodeVerificationResult verifyCode(String email, String codeHash) {
        Long result = redisTemplate.execute(
                VERIFY_CODE_SCRIPT,
                List.of(codeKey(email), attemptsKey(email), cooldownKey(email)),
                codeHash,
                String.valueOf(MAX_CONFIRM_ATTEMPTS)
        );
        if (Long.valueOf(1L).equals(result)) {
            return PasswordResetCodeVerificationResult.SUCCESS;
        }
        if (Long.valueOf(2L).equals(result)) {
            return PasswordResetCodeVerificationResult.INVALID;
        }
        if (Long.valueOf(3L).equals(result)) {
            return PasswordResetCodeVerificationResult.ATTEMPTS_EXCEEDED;
        }
        return PasswordResetCodeVerificationResult.EXPIRED;
    }

    // 원문 재설정 토큰의 해시를 키로 사용하고 인증된 이메일을 TTL 동안 저장한다.
    public void saveToken(String tokenHash, String email, Duration ttl) {
        redisTemplate.opsForValue().set(tokenKey(tokenHash), email, ttl);
    }

    // 유효한 재설정 토큰의 사용권을 원자적으로 선점하고 연결된 이메일을 반환한다.
    public Optional<String> acquireTokenUse(String tokenHash) {
        return Optional.ofNullable(redisTemplate.execute(
                ACQUIRE_TOKEN_SCRIPT,
                List.of(tokenKey(tokenHash), tokenLockKey(tokenHash))
        ));
    }

    // DB 트랜잭션 커밋 후 선점한 재설정 토큰을 최종 소비한다.
    public void completeTokenUse(String tokenHash) {
        redisTemplate.execute(
                COMPLETE_TOKEN_SCRIPT,
                List.of(tokenKey(tokenHash), tokenLockKey(tokenHash))
        );
    }

    // DB 트랜잭션 롤백 시 잠금만 제거해 원래 재설정 토큰을 다시 사용할 수 있게 한다.
    public void releaseTokenUse(String tokenHash) {
        redisTemplate.delete(tokenLockKey(tokenHash));
    }

    private String codeKey(String email) {
        return CODE_KEY_PREFIX + email;
    }

    private String attemptsKey(String email) {
        return ATTEMPTS_KEY_PREFIX + email;
    }

    private String cooldownKey(String email) {
        return COOLDOWN_KEY_PREFIX + email;
    }

    private String tokenKey(String tokenHash) {
        return TOKEN_KEY_PREFIX + tokenHash;
    }

    private String tokenLockKey(String tokenHash) {
        return TOKEN_LOCK_KEY_PREFIX + tokenHash;
    }
}
