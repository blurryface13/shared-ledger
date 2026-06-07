package com.spvermicelli.tripledger.identity.infrastructure.wechat;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.wechat.mini-program")
public class WechatMiniProgramProperties implements InitializingBean {

    private boolean mockEnabled;
    private boolean manualMobileEnabled;
    private String appId;
    private String appSecret;
    private String code2SessionUrl;
    private String stableAccessTokenUrl;
    private String getPhoneNumberUrl;

    public boolean isMockEnabled() {
        return mockEnabled;
    }

    public void setMockEnabled(boolean mockEnabled) {
        this.mockEnabled = mockEnabled;
    }

    public boolean isManualMobileEnabled() {
        return manualMobileEnabled;
    }

    public void setManualMobileEnabled(boolean manualMobileEnabled) {
        this.manualMobileEnabled = manualMobileEnabled;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getCode2SessionUrl() {
        return code2SessionUrl;
    }

    public void setCode2SessionUrl(String code2SessionUrl) {
        this.code2SessionUrl = code2SessionUrl;
    }

    public String getStableAccessTokenUrl() {
        return stableAccessTokenUrl;
    }

    public void setStableAccessTokenUrl(String stableAccessTokenUrl) {
        this.stableAccessTokenUrl = stableAccessTokenUrl;
    }

    public String getGetPhoneNumberUrl() {
        return getPhoneNumberUrl;
    }

    public void setGetPhoneNumberUrl(String getPhoneNumberUrl) {
        this.getPhoneNumberUrl = getPhoneNumberUrl;
    }

    /**
     * 生产态配置校验。
     * 当 mock 关闭时，必须显式提供微信小程序凭据，避免线上误用空配置启动。
     */
    @Override
    public void afterPropertiesSet() {
        if (mockEnabled) {
            return;
        }
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)) {
            throw new IllegalStateException("微信小程序 appId/appSecret 未配置");
        }
        if (!StringUtils.hasText(code2SessionUrl)) {
            throw new IllegalStateException("微信 code2Session 接口地址未配置");
        }
        if (!StringUtils.hasText(stableAccessTokenUrl)) {
            throw new IllegalStateException("微信 stable access token 接口地址未配置");
        }
        if (!StringUtils.hasText(getPhoneNumberUrl)) {
            throw new IllegalStateException("微信手机号接口地址未配置");
        }
    }
}
