package com.spvermicelli.tripledger.shared.infrastructure.redis;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RedisConcurrencyProperties.class)
public class RedisConcurrencyConfiguration {
}
