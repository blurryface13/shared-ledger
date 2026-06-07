package com.spvermicelli.tripledger.identity.interfaces.rest.auth.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 微信登录请求。
 * 小程序端只允许上传 wx.login()/uni.login() 获取到的 code。
 * openid、unionId 等敏感身份字段必须由后端自行向微信服务端换取。
 */
@Getter
@Setter
public class WechatLoginRequest {

    /**
     * 微信登录 code。
     * 这是前端唯一允许传入的登录凭证。
     */
    @NotBlank(message = "code 不能为空")
    private String code;

    /**
     * 当后端识别到该 openId 对应账号已注销时，
     * 前端可再次调用本接口并显式确认重新注册/恢复登录。
     */
    private Boolean confirmReRegister;
}
