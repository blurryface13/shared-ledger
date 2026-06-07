package com.spvermicelli.tripledger.identity.interfaces.rest.auth.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 绑定手机号请求。
 * 生产环境默认使用微信手机号授权返回的 phoneCode。
 * manualMobile 仅用于开发/体验排障场景，必须由服务端环境变量显式开启。
 */
@Getter
@Setter
public class BindWechatMobileRequest {

    /**
     * 绑定时提交的用户名。
     * 生产链路要求在首登绑定阶段完成用户名资料补齐。
     */
    @Size(max = 20, message = "nickname 长度不能超过20个字符")
    private String nickname;

    /**
     * 绑定时提交的头像地址。
     * 可为空；为空时用户头像保持空值，后续可在“我的”页面上传。
     */
    @Size(max = 500, message = "avatarUrl 长度不能超过500个字符")
    private String avatarUrl;

    /**
     * 微信小程序手机号能力返回的临时 code。
     * 后端会基于该 code 向微信服务端换取真实手机号。
     */
    @Size(max = 128, message = "phoneCode 长度不能超过128个字符")
    private String phoneCode;

    /**
     * 手动输入手机号，仅在 app.wechat.mini-program.manual-mobile-enabled=true 时允许。
     */
    @Pattern(regexp = "^$|^1\\d{10}$", message = "manualMobile 必须为11位手机号")
    private String manualMobile;
}
