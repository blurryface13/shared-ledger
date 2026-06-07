package com.spvermicelli.tripledger.identity.domain.user.service;

/**
 * 微信手机号解析网关。
 * 用于把前端上传的手机号绑定凭据解析为可入库的手机号。
 */
public interface WechatMobileGateway {

    /**
     * 解析手机号。
     * 当前优先支持小程序微信手机号 code；
     * 同时为未来 App 场景预留手动手机号入参。
     */
    String resolveMobile(String phoneCode, String manualMobile);
}
