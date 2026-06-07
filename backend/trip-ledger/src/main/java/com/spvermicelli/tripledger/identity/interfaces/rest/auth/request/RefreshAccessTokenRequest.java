package com.spvermicelli.tripledger.identity.interfaces.rest.auth.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * refresh token 换发请求。
 */
@Getter
@Setter
public class RefreshAccessTokenRequest {

    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}
