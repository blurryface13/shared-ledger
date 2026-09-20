package com.spvermicelli.tripledger.shared.infrastructure.redis;

import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import java.time.Duration;
import java.util.UUID;
import java.util.Collections;
import java.util.function.Supplier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Service
public class RedisIdempotencyService {

    private static final DefaultRedisScript<Long> RELEASE_OWNED_MARKER = new DefaultRedisScript<>(
        """
        if redis.call('get', KEYS[1]) == ARGV[1] then
            return redis.call('del', KEYS[1])
        end
        return 0
        """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final RedisConcurrencyProperties properties;

    public RedisIdempotencyService(StringRedisTemplate redisTemplate, RedisConcurrencyProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public <T> T executeOnce(String businessKey, Duration ttl, Supplier<T> action) {
        String redisKey = properties.getIdempotencyPrefix() + businessKey;
        String value = UUID.randomUUID().toString();
        Boolean created = redisTemplate.opsForValue().setIfAbsent(redisKey, value, ttl);
        if (!Boolean.TRUE.equals(created)) {
            throw new BusinessException(ErrorCode.CONFLICT, "请求正在处理或已提交，请勿重复操作");
        }

        try {
            return action.get();
        } catch (RuntimeException exception) {
            try {
                // The lease may have expired and been acquired by another request.
                redisTemplate.execute(RELEASE_OWNED_MARKER, Collections.singletonList(redisKey), value);
            } catch (RuntimeException cleanupFailure) {
                // Preserve the business failure; the marker still has a bounded TTL.
                exception.addSuppressed(cleanupFailure);
            }
            throw exception;
        }
    }
}
