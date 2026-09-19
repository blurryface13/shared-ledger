package com.spvermicelli.tripledger.shared.infrastructure.redis;

import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisIdempotencyService {

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
            redisTemplate.delete(redisKey);
            throw exception;
        }
    }
}
