package com.spvermicelli.tripledger.identity.application.auth.result;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

/**
 * 微信登录结果。
 */
@Getter
@Builder
public class WechatLoginResult {
    private String token;
    private String refreshToken;
    private String bindToken;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Boolean mobileBound;
    private Boolean needBindMobile;
    private Boolean cancelledUser;
    private Boolean needConfirmReRegister;
    private Instant tokenExpireAt;
    private Instant refreshTokenExpireAt;
}
