package com.spvermicelli.tripledger.identity.interfaces.rest.auth.response;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

/**
 * 绑定微信手机号响应。
 */
@Getter
@Builder
public class BindWechatMobileResponse {
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
