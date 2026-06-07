package com.spvermicelli.tripledger.identity.interfaces.rest.auth.response;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

/**
 * 微信登录响应。
 */
@Getter
@Builder
public class WechatLoginResponse {
    // 数据库里的 userid
    private Long userId;
    // access token，访问令牌
    private String token;
    // 刷新令牌
    private String refreshToken;
    // 专门给首次登陆，未绑定手机号的临时令牌，用于绑定手机号操作
    private String bindToken;
    // 用户的昵称
    private String nickname;
    // 头像地址
    private String avatarUrl;
    // 是否已经绑定手机号，已绑定则为true
    private Boolean mobileBound;
    // 本次登陆是否还需要绑定手机号，false表示不需要，直接进入系统
    private Boolean needBindMobile;
    // 当前 openid 对应账号是否处于已注销状态
    private Boolean cancelledUser;
    // 前端是否需要再次确认重新注册/恢复登录
    private Boolean needConfirmReRegister;
    // access token 过期时间
    private Instant tokenExpireAt;
    // refresh token 过期时间
    private Instant refreshTokenExpireAt;
}
