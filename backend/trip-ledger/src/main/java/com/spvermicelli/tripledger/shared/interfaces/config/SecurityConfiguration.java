package com.spvermicelli.tripledger.shared.interfaces.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 基础配置。
 * 当前项目真正的认证由 MVC 拦截器中的 JWT 逻辑完成，
 * 这里主要负责关闭默认表单登录/Basic Auth，并保留统一的安全过滤器链能力。
 */
@Configuration
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity,
        @org.springframework.beans.factory.annotation.Value("${management.server.port:-1}") int managementPort) throws Exception {
        return httpSecurity
            .csrf(csrf -> csrf.disable())
            .formLogin(formLogin -> formLogin.disable())
            .httpBasic(httpBasic -> httpBasic.disable())
            .logout(logout -> logout.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/health", "/actuator/prometheus").access((authentication, context) ->
                    new org.springframework.security.authorization.AuthorizationDecision(
                        managementPort > 0 && context.getRequest().getLocalPort() == managementPort))
                .requestMatchers("/actuator", "/actuator/**").denyAll()
                .anyRequest().permitAll())
            .cors(Customizer.withDefaults())
            .build();
    }
}
