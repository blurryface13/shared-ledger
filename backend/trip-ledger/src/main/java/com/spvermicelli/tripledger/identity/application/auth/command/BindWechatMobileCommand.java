package com.spvermicelli.tripledger.identity.application.auth.command;

import lombok.Builder;
import lombok.Getter;

/**
 * 绑定微信手机号命令。
 */
@Getter
@Builder
public class BindWechatMobileCommand {
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String phoneCode;
    private String manualMobile;
}
