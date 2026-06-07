package com.spvermicelli.tripledger.identity.application.auth.result;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

/**
 * 绑定微信手机号结果。
 */
@Getter
@Builder
public class BindWechatMobileResult {
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String mobile;
    private String mobileMasked;
    private Boolean mobileBound;
    private String token;
    private String refreshToken;
    private Instant tokenExpireAt;
    private Instant refreshTokenExpireAt;
}
