package com.spvermicelli.tripledger.shared.infrastructure.redis;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.data.redis.core.script.RedisScript;

import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Uses only a random namespaced key; never flushes shared Redis data. */
class RedisIdempotencyServiceTest {
    private LettuceConnectionFactory factory;
    private StringRedisTemplate redis;
    private RedisIdempotencyService service;
    private String redisKey;
    private final Duration ttl = Duration.ofMinutes(1);

    @BeforeEach
    void setUp() {
        var config = new RedisStandaloneConfiguration(
            System.getenv().getOrDefault("SPRING_DATA_REDIS_HOST", "localhost"),
            Integer.parseInt(System.getenv().getOrDefault("SPRING_DATA_REDIS_PORT", "6379")));
        String password = System.getenv("SPRING_DATA_REDIS_PASSWORD");
        if (password != null && !password.isEmpty()) config.setPassword(password);
        String username = System.getenv("SPRING_DATA_REDIS_USERNAME");
        if (username != null && !username.isEmpty()) config.setUsername(username);
        config.setDatabase(Integer.parseInt(System.getenv().getOrDefault("SPRING_DATA_REDIS_DATABASE", "0")));
        factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();
        factory.start();
        redis = new StringRedisTemplate(factory);
        var properties = new RedisConcurrencyProperties();
        properties.setIdempotencyPrefix("trip-ledger:test:" + UUID.randomUUID() + ":");
        redisKey = properties.getIdempotencyPrefix() + "operation";
        service = new RedisIdempotencyService(redis, properties);
    }

    @AfterEach
    void tearDown() {
        try { if (redis != null) redis.delete(redisKey); }
        finally { if (factory != null) factory.destroy(); }
    }

    @Test
    void completedRequestRejectsDuplicateWithoutExecutingIt() {
        var calls = new AtomicInteger();
        assertEquals(1, service.executeOnce("operation", ttl, calls::incrementAndGet));
        assertThrows(BusinessException.class,
            () -> service.executeOnce("operation", ttl, calls::incrementAndGet));
        assertEquals(1, calls.get());
    }

    @Test
    void failedOwnerCanRetryAndKeepsOriginalException() {
        var failure = new IllegalStateException("business failure");
        assertSame(failure, assertThrows(IllegalStateException.class,
            () -> service.executeOnce("operation", ttl, () -> { throw failure; })));
        assertEquals("retried", service.executeOnce("operation", ttl, () -> "retried"));
    }

    @Test
    void expiredOwnerMustNotDeleteSuccessorMarker() {
        var failure = new IllegalStateException("old request failed");
        assertSame(failure, assertThrows(IllegalStateException.class,
            () -> service.executeOnce("operation", ttl, () -> {
                // Deterministically expire the old lease before another request acquires it.
                redis.expire(redisKey, Duration.ZERO);
                assertEquals("successor", service.executeOnce("operation", ttl, () -> "successor"));
                throw failure;
            })));
        assertThrows(BusinessException.class,
            () -> service.executeOnce("operation", ttl, () -> "must not execute"));
    }

    @Test
    void cleanupFailureDoesNotMaskBusinessFailure() {
        var failingRedis = spy(redis);
        var cleanupFailure = new IllegalStateException("Redis unavailable during cleanup");
        doThrow(cleanupFailure).when(failingRedis).execute(
            org.mockito.ArgumentMatchers.<RedisScript<Long>>any(), anyList(), anyString());
        var properties = new RedisConcurrencyProperties();
        properties.setIdempotencyPrefix(redisKey.substring(0, redisKey.length() - "operation".length()));
        var failingService = new RedisIdempotencyService(failingRedis, properties);
        var businessFailure = new IllegalArgumentException("invalid business operation");
        assertSame(businessFailure, assertThrows(IllegalArgumentException.class,
            () -> failingService.executeOnce("operation", ttl, () -> { throw businessFailure; })));
        assertArrayEquals(new Throwable[] {cleanupFailure}, businessFailure.getSuppressed());
        assertNotNull(redis.opsForValue().get(redisKey));
        assertTrue(redis.getExpire(redisKey) > 0);
    }

    @Test
    void concurrentRequestCannotEnterWhileOwnerIsRunning() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var calls = new AtomicInteger();
        try (var executor = Executors.newSingleThreadExecutor()) {
            var owner = executor.submit(() -> service.executeOnce("operation", ttl, () -> {
                calls.incrementAndGet();
                entered.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("test timeout");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
                return "owner";
            }));
            try {
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                assertThrows(BusinessException.class,
                    () -> service.executeOnce("operation", ttl, calls::incrementAndGet));
            } finally { release.countDown(); }
            assertEquals("owner", owner.get(5, TimeUnit.SECONDS));
            assertEquals(1, calls.get());
        }
    }
}
