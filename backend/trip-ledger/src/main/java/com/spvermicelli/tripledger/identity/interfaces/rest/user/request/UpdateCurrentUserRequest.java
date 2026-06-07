package com.spvermicelli.tripledger.identity.interfaces.rest.user.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 更新当前用户资料请求。
 * 四个字段均可选，但至少需要传一个有值字段。
 * 生产环境手机号更新默认使用微信手机号授权返回的 phoneCode。
 * manualMobile 仅在服务端显式开启手动手机号能力时允许。
 */
@Getter
@Setter
public class UpdateCurrentUserRequest {
    @Size(max = 20, message = "nickname 长度不能超过20个字符")
    private String nickname;
    private String avatarUrl;

    @Size(max = 128, message = "phoneCode 长度不能超过128个字符")
    private String phoneCode;

    @Pattern(regexp = "^$|^1\\d{10}$", message = "manualMobile 必须为11位手机号")
    private String manualMobile;
}
