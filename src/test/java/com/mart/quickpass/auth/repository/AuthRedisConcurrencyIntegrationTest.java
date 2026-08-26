package com.mart.quickpass.auth.repository;

import com.mart.quickpass.global.config.JwtProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@Testcontainers
class AuthRedisConcurrencyIntegrationTest {

    private static final int REDIS_PORT = 6379;
    private static final int CONCURRENT_REQUESTS = 20;

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(REDIS_PORT);

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;
    private static RefreshTokenRepository refreshTokenRepository;
    private static PasswordResetRepository passwordResetRepository;

    @BeforeAll
    static void setUpRedis() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(REDIS_PORT));
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        refreshTokenRepository = new RefreshTokenRepository(
                redisTemplate,
                new JwtProperties("01234567890123456789012345678901", 60_000, 120_000)
        );
        passwordResetRepository = new PasswordResetRepository(redisTemplate);
    }

    @AfterAll
    static void tearDownRedis() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @BeforeEach
    void clearRedis() {
        connectionFactory.getConnection().serverCommands().flushDb();
    }

    @Test
    void exactlyOneConcurrentRefreshRotationSucceeds() throws Exception {
        Long userId = 1L;
        String currentToken = "refresh-current";
        refreshTokenRepository.save(userId, currentToken);

        List<Boolean> results = runConcurrently(CONCURRENT_REQUESTS, index ->
                refreshTokenRepository.rotateIfMatches(userId, currentToken, "refresh-new-" + index));

        assertThat(results).containsExactlyInAnyOrderElementsOf(expectedOneSuccess(CONCURRENT_REQUESTS));
        String storedToken = redisTemplate.opsForValue().get("refreshToken:" + userId);
        assertThat(storedToken).startsWith("refresh-new-");
        assertThat(refreshTokenRepository.rotateIfMatches(userId, currentToken, "another-token")).isFalse();
    }

    @Test
    void previousTokenLogoutCannotDeleteCurrentToken() throws Exception {
        Long userId = 1L;
        refreshTokenRepository.save(userId, "refresh-current");

        List<Boolean> results = runConcurrently(CONCURRENT_REQUESTS, index ->
                refreshTokenRepository.deleteIfMatches(userId, "refresh-previous-" + index));

        assertThat(results).containsOnly(false);
        assertThat(redisTemplate.opsForValue().get("refreshToken:" + userId))
                .isEqualTo("refresh-current");
    }

    @Test
    void fiveConcurrentWrongCodesInvalidateCodeExactlyOnce() throws Exception {
        String email = "user@example.com";
        passwordResetRepository.acquireCooldown(email, Duration.ofMinutes(1));
        passwordResetRepository.saveCode(email, "correct-hash", Duration.ofMinutes(10));

        List<PasswordResetCodeVerificationResult> results = runConcurrently(5, index ->
                passwordResetRepository.verifyCode(email, "wrong-hash-" + index));

        assertThat(results).containsExactlyInAnyOrder(
                PasswordResetCodeVerificationResult.INVALID,
                PasswordResetCodeVerificationResult.INVALID,
                PasswordResetCodeVerificationResult.INVALID,
                PasswordResetCodeVerificationResult.INVALID,
                PasswordResetCodeVerificationResult.ATTEMPTS_EXCEEDED
        );
        assertThat(passwordResetRepository.verifyCode(email, "correct-hash"))
                .isEqualTo(PasswordResetCodeVerificationResult.EXPIRED);
        assertThat(passwordResetRepository.acquireCooldown(email, Duration.ofMinutes(1))).isTrue();
    }

    @Test
    void exactlyOneConcurrentPasswordResetTokenUseAcquiresLock() throws Exception {
        String tokenHash = "reset-token-hash";
        String email = "user@example.com";
        passwordResetRepository.saveToken(tokenHash, email, Duration.ofMinutes(5));

        List<Optional<String>> results = runConcurrently(CONCURRENT_REQUESTS, index ->
                passwordResetRepository.acquireTokenUse(tokenHash));

        assertThat(results.stream().filter(Optional::isPresent)).hasSize(1);
        assertThat(results.stream().flatMap(Optional::stream)).containsExactly(email);

        passwordResetRepository.releaseTokenUse(tokenHash);
        assertThat(passwordResetRepository.acquireTokenUse(tokenHash)).contains(email);
        passwordResetRepository.completeTokenUse(tokenHash);
        assertThat(passwordResetRepository.acquireTokenUse(tokenHash)).isEmpty();
    }

    private static List<Boolean> expectedOneSuccess(int size) {
        List<Boolean> expected = new ArrayList<>();
        expected.add(true);
        for (int i = 1; i < size; i++) {
            expected.add(false);
        }
        return expected;
    }

    private static <T> List<T> runConcurrently(int count, IndexedTask<T> task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (int index = 0; index < count; index++) {
                int taskIndex = index;
                Callable<T> callable = () -> {
                    ready.countDown();
                    start.await();
                    return task.run(taskIndex);
                };
                futures.add(executor.submit(callable));
            }
            ready.await();
            start.countDown();

            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    @FunctionalInterface
    private interface IndexedTask<T> {
        T run(int index);
    }
}
