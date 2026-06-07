package com.spvermicelli.tripledger.shared.interfaces.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JSON 配置。
 * 当前项目在应用层需要手动序列化申请快照、结算摘要和导出内容，
 * 因此显式提供一个 Jackson ObjectMapper Bean，避免不同环境下自动配置差异造成注入失败。
 */
@Configuration
public class JsonConfiguration {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
