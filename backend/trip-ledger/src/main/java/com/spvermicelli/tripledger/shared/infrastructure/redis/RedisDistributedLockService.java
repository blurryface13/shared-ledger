package com.spvermicelli.tripledger.shared.infrastructure.redis;

import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class RedisDistributedLockService {

    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
        """
        if redis.call('get', KEYS[1]) == ARGV[1] then
            return redis.call('del', KEYS[1])
        end
        return 0
        """,
        Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final RedisConcurrencyProperties properties;

    public RedisDistributedLockService(StringRedisTemplate redisTemplate, RedisConcurrencyProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public <T> T executeWithBookLock(Long bookId, Supplier<T> action) {
        return executeWithLock("book:" + bookId, Duration.ofSeconds(3), Duration.ofSeconds(20), action);
    }

    public <T> T executeWithLock(String lockKey, Duration waitTimeout, Duration leaseTime, Supplier<T> action) {
        String redisKey = properties.getLockPrefix() + lockKey;
        String lockValue = UUID.randomUUID().toString();
        long deadline = System.nanoTime() + waitTimeout.toNanos();

        while (System.nanoTime() < deadline) {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(redisKey, lockValue, leaseTime);
            if (Boolean.TRUE.equals(acquired)) {
                try {
                    return action.get();
                } finally {
                    redisTemplate.execute(RELEASE_LOCK_SCRIPT, Collections.singletonList(redisKey), lockValue);
                }
            }
            sleepQuietly();
        }

        throw new BusinessException(ErrorCode.CONFLICT, "当前账本正在被其他请求修改，请稍后重试");
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(80);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "等待分布式锁时被中断");
        }
    }
}
