package com.mart.quickpass.cart.repository;

import com.mart.quickpass.cart.dto.CartItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

/**
 * qrCode 기준으로 장바구니 아이템(Hash: barcode → {name, price, quantity})을 Redis에 보관하는 리포지토리.
 */
@Repository
@RequiredArgsConstructor
public class CartItemsRepository {

    private static final String KEY_PREFIX = "cart:items:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<CartItem> findItem(String qrCode, String barcode) {
        String value = hashOps().get(key(qrCode), barcode);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.readValue(value, CartItem.class));
    }

    public void saveItem(String qrCode, String barcode, CartItem item) {
        hashOps().put(key(qrCode), barcode, objectMapper.writeValueAsString(item));
    }

    public void deleteItem(String qrCode, String barcode) {
        hashOps().delete(key(qrCode), barcode);
    }

    /** 장바구니의 아이템을 전부 비운다 (Hash 키 자체를 삭제). */
    public void deleteAll(String qrCode) {
        redisTemplate.delete(key(qrCode));
    }

    /** 유휴 타임아웃을 초기화한다 (sliding TTL). 아이템이 하나도 없으면 아무 효과가 없다. */
    public void refreshTtl(String qrCode, Duration ttl) {
        redisTemplate.expire(key(qrCode), ttl);
    }

    private HashOperations<String, String, String> hashOps() {
        return redisTemplate.opsForHash();
    }

    private String key(String qrCode) {
        return KEY_PREFIX + qrCode;
    }
}
