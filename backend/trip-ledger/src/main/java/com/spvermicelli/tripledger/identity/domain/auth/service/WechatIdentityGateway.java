package com.spvermicelli.tripledger.identity.domain.auth.service;

import com.spvermicelli.tripledger.identity.domain.auth.model.WechatIdentity;

/**
 * 微信身份解析网关。
 * 当前先定义“小程序登录 code -> openid/unionid”的能力，
 * 后续 App 外部拉起微信授权时，可以继续在 infrastructure 中扩展新的实现。
 */
public interface WechatIdentityGateway {

    WechatIdentity resolveMiniProgramIdentity(String code);
}
