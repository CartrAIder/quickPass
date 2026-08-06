package com.mart.quickpass.cart.repository;

import com.mart.quickpass.cart.dto.CartSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;
import java.util.List;

// redis 카트 세션 레포지토리
@Repository
@RequiredArgsConstructor
public class CartSessionRepository {

    private static final String KEY_PREFIX = "cart:session:";
    private static final String USER_KEY_PREFIX = "user:cart:";
    private static final String ITEMS_KEY_PREFIX = "cart:items:";

    private static final DefaultRedisScript<Long> CLAIM_SCRIPT = new DefaultRedisScript<>("""
            local cartSession = redis.call('GET', KEYS[1])
            local userCart = redis.call('GET', KEYS[2])
            if cartSession then
                local ownerId = tostring(cjson.decode(cartSession).userId)
                if ownerId ~= ARGV[1] then return 2 end
                if userCart and userCart ~= ARGV[2] then return 3 end
                redis.call('PEXPIRE', KEYS[1], ARGV[4])
                redis.call('SET', KEYS[2], ARGV[2], 'PX', ARGV[4])
                redis.call('PEXPIRE', KEYS[3], ARGV[4])
                return 1
            end
            if userCart and userCart ~= ARGV[2] then return 3 end
            redis.call('SET', KEYS[1], ARGV[3], 'PX', ARGV[4])
            redis.call('SET', KEYS[2], ARGV[2], 'PX', ARGV[4])
            return 0
            """, Long.class);

    private static final DefaultRedisScript<Long> REFRESH_TTL_SCRIPT = new DefaultRedisScript<>("""
            local refreshed = 0
            for i = 1, #KEYS do
                refreshed = refreshed + redis.call('PEXPIRE', KEYS[i], ARGV[1])
            end
            return refreshed
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /** 카트 점유와 사용자 -> 카트 역방향 인덱스를 원자적으로 생성/갱신한다. */
    public CartClaimResult claimOrResume(String qrCode, CartSession session, Duration ttl) {
        Long result = redisTemplate.execute(
                CLAIM_SCRIPT,
                List.of(key(qrCode), userKey(session.userId()), itemsKey(qrCode)),
                String.valueOf(session.userId()),
                qrCode,
                objectMapper.writeValueAsString(session),
                String.valueOf(ttl.toMillis()));
        return switch (result == null ? -1 : result.intValue()) {
            case 0 -> CartClaimResult.CREATED;
            case 1 -> CartClaimResult.RESUMED;
            case 2 -> CartClaimResult.CART_CONFLICT;
            case 3 -> CartClaimResult.USER_CONFLICT;
            default -> throw new IllegalStateException("카트 세션 점유 결과를 확인할 수 없습니다.");
        };
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

    public Optional<String> findQrCodeByUserId(Long userId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(userKey(userId)));
    }

    public void deleteUserCart(Long userId) {
        redisTemplate.delete(userKey(userId));
    }

    /** 세션성 카트 키의 sliding TTL을 하나의 Redis 명령으로 갱신한다. */
    public void refreshSessionTtl(Long userId, String qrCode, Duration ttl) {
        redisTemplate.execute(
                REFRESH_TTL_SCRIPT,
                List.of(key(qrCode), userKey(userId), itemsKey(qrCode)),
                String.valueOf(ttl.toMillis()));
    }

    private String key(String qrCode) {
        return KEY_PREFIX + qrCode;
    }

    private String userKey(Long userId) {
        return USER_KEY_PREFIX + userId;
    }

    private String itemsKey(String qrCode) {
        return ITEMS_KEY_PREFIX + qrCode;
    }
}
