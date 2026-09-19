package com.spvermicelli.tripledger.shared.infrastructure.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.redis")
public class RedisConcurrencyProperties {

    private String lockPrefix = "trip-ledger:lock:";
    private String idempotencyPrefix = "trip-ledger:idempotency:";

    public String getLockPrefix() {
        return lockPrefix;
    }

    public void setLockPrefix(String lockPrefix) {
        this.lockPrefix = lockPrefix;
    }

    public String getIdempotencyPrefix() {
        return idempotencyPrefix;
    }

    public void setIdempotencyPrefix(String idempotencyPrefix) {
        this.idempotencyPrefix = idempotencyPrefix;
    }
}
