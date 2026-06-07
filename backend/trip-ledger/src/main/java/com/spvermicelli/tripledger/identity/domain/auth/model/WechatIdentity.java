package com.spvermicelli.tripledger.identity.domain.auth.model;

/**
 * 微信身份信息。
 * 该对象由后端携带登录 code 向微信服务端换取后得到，
 * 用于隔离微信开放平台返回报文与内部领域模型。
 *
 * @param openId 小程序用户在当前微信应用下的唯一身份标识
 * @param unionId 微信开放平台统一身份标识，可能为空
 */
public record WechatIdentity(String openId, String unionId) {
}
