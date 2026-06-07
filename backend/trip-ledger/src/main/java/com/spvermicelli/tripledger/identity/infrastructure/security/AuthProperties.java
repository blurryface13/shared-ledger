package com.spvermicelli.tripledger.identity.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties implements InitializingBean {

    private boolean enabled = true;
    private String headerName = "Authorization";
    private String tokenPrefix = "Bearer";
    private String jwtIssuer = "trip-ledger";
    private String jwtSecret;
    private long accessTokenExpireSeconds = 604800L;
    private long bindTokenExpireSeconds = 600L;
    private long refreshTokenExpireSeconds = 2592000L;
    private List<String> excludePaths = new ArrayList<>();
    private List<String> bindMobilePaths = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public String getTokenPrefix() {
        return tokenPrefix;
    }

    public void setTokenPrefix(String tokenPrefix) {
        this.tokenPrefix = tokenPrefix;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public String getJwtIssuer() {
        return jwtIssuer;
    }

    public void setJwtIssuer(String jwtIssuer) {
        this.jwtIssuer = jwtIssuer;
    }

    public long getAccessTokenExpireSeconds() {
        return accessTokenExpireSeconds;
    }

    public void setAccessTokenExpireSeconds(long accessTokenExpireSeconds) {
        this.accessTokenExpireSeconds = accessTokenExpireSeconds;
    }

    public long getBindTokenExpireSeconds() {
        return bindTokenExpireSeconds;
    }

    public void setBindTokenExpireSeconds(long bindTokenExpireSeconds) {
        this.bindTokenExpireSeconds = bindTokenExpireSeconds;
    }

    public long getRefreshTokenExpireSeconds() {
        return refreshTokenExpireSeconds;
    }

    public void setRefreshTokenExpireSeconds(long refreshTokenExpireSeconds) {
        this.refreshTokenExpireSeconds = refreshTokenExpireSeconds;
    }

    public List<String> getExcludePaths() {
        return excludePaths;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }

    public List<String> getBindMobilePaths() {
        return bindMobilePaths;
    }

    public void setBindMobilePaths(List<String> bindMobilePaths) {
        this.bindMobilePaths = bindMobilePaths;
    }

    /**
     * 启动期校验认证配置。
     * 生产系统里，认证密钥和过期时间必须在启动阶段就校验失败，而不是等第一笔请求来了才暴露问题。
     */
    @Override
    public void afterPropertiesSet() {
        if (!enabled) {
            return;
        }
        if (!StringUtils.hasText(jwtSecret) || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.auth.jwt-secret 未配置或长度不足 32 字节");
        }
        if (!StringUtils.hasText(jwtIssuer)) {
            throw new IllegalStateException("app.auth.jwt-issuer 不能为空");
        }
        if (accessTokenExpireSeconds <= 0 || bindTokenExpireSeconds <= 0 || refreshTokenExpireSeconds <= 0) {
            throw new IllegalStateException("认证 token 过期时间必须大于 0");
        }
        if (refreshTokenExpireSeconds <= accessTokenExpireSeconds) {
            throw new IllegalStateException("refresh token 有效期必须长于 access token");
        }
    }
}
