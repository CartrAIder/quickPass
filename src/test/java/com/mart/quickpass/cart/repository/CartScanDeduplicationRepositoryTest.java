package com.mart.quickpass.cart.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class CartScanDeduplicationRepositoryTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final CartScanDeduplicationRepository repository =
            new CartScanDeduplicationRepository(redisTemplate);

    @Test
    void tryMarkProcessedAtomicallyStoresScanIdWithTtl() {
        Duration ttl = Duration.ofHours(2);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent("cart:scan:dedup:cart_001:scan-1", "true", ttl))
                .thenReturn(true);

        boolean firstProcessing = repository.tryMarkProcessed("cart_001", "scan-1", ttl);

        assertThat(firstProcessing).isTrue();
        verify(valueOperations).setIfAbsent("cart:scan:dedup:cart_001:scan-1", "true", ttl);
    }
}
