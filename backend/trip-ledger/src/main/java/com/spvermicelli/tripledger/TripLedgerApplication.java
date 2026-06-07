package com.spvermicelli.tripledger;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

/**
 * 应用启动入口。
 * 当前项目的认证完全由自定义 JWT 方案负责，因此显式排除默认的 UserDetailsService 自动配置，
 * 避免生成无意义的开发密码日志，减少安全噪音和误导。
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
@MapperScan(basePackages = {
    "com.spvermicelli.tripledger.identity.infrastructure.persistence.mapper",
    "com.spvermicelli.tripledger.ledger.infrastructure.persistence.mapper",
    "com.spvermicelli.tripledger.billing.infrastructure.persistence.mapper",
    "com.spvermicelli.tripledger.settlement.infrastructure.persistence.mapper",
    "com.spvermicelli.tripledger.export.infrastructure.persistence.mapper",
    "com.spvermicelli.tripledger.shared.infrastructure.persistence.mapper"
})
public class TripLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TripLedgerApplication.class, args);
    }
}
