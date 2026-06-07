package com.spvermicelli.tripledger.identity.application.auth.command;

import lombok.Builder;
import lombok.Getter;

/**
 * 微信登录命令。
 */
@Getter
@Builder
public class WechatLoginCommand {
    private String code;
    private Boolean confirmReRegister;
}
