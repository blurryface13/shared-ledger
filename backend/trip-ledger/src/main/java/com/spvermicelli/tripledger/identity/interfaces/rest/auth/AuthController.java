package com.spvermicelli.tripledger.identity.interfaces.rest.auth;

import com.spvermicelli.tripledger.identity.application.auth.AuthApplicationService;
import com.spvermicelli.tripledger.identity.application.auth.command.BindWechatMobileCommand;
import com.spvermicelli.tripledger.identity.application.auth.command.RefreshAccessTokenCommand;
import com.spvermicelli.tripledger.identity.application.auth.command.WechatLoginCommand;
import com.spvermicelli.tripledger.identity.application.auth.result.BindWechatMobileResult;
import com.spvermicelli.tripledger.identity.application.auth.result.RefreshAccessTokenResult;
import com.spvermicelli.tripledger.identity.application.auth.result.WechatLoginResult;
import com.spvermicelli.tripledger.identity.interfaces.rest.auth.request.BindWechatMobileRequest;
import com.spvermicelli.tripledger.identity.interfaces.rest.auth.request.RefreshAccessTokenRequest;
import com.spvermicelli.tripledger.identity.interfaces.rest.auth.request.WechatLoginRequest;
import com.spvermicelli.tripledger.identity.interfaces.rest.auth.response.BindWechatMobileResponse;
import com.spvermicelli.tripledger.identity.interfaces.rest.auth.response.RefreshAccessTokenResponse;
import com.spvermicelli.tripledger.identity.interfaces.rest.auth.response.WechatLoginResponse;
import com.spvermicelli.tripledger.shared.common.context.UserContextHolder;
import com.spvermicelli.tripledger.shared.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthApplicationService authApplicationService;

    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    /**
     * 微信登录接口。
     * 当前小程序端仅允许传递 uni.login()/wx.login() 返回的 code。
     * 后端会基于该 code 进一步向微信服务端换取 openid/unionid，
     * 并据此决定是否为新用户、是否需要强制绑定手机号。
     */
    @PostMapping("/wechat-login")
    public ApiResponse<WechatLoginResponse> wechatLogin(@Valid @RequestBody WechatLoginRequest request) {
        WechatLoginResult result = authApplicationService.wechatLogin(WechatLoginCommand.builder()
            .code(request.getCode())
            .confirmReRegister(request.getConfirmReRegister())
            .build());

        return ApiResponse.success(WechatLoginResponse.builder()
            .token(result.getToken())
            .refreshToken(result.getRefreshToken())
            .bindToken(result.getBindToken())
            .userId(result.getUserId())
            .nickname(result.getNickname())
            .avatarUrl(result.getAvatarUrl())
            .mobileBound(result.getMobileBound())
            .needBindMobile(result.getNeedBindMobile())
            .cancelledUser(result.getCancelledUser())
            .needConfirmReRegister(result.getNeedConfirmReRegister())
            .tokenExpireAt(result.getTokenExpireAt())
            .refreshTokenExpireAt(result.getRefreshTokenExpireAt())
            .build());
    }

    /**
     * 绑定微信手机号。
     * 新用户在首登后通过 bind token 完成手机号绑定；已登录用户也可通过 access token 重走该接口进行补绑。
     */
    @PostMapping("/bind-wechat-mobile")
    public ApiResponse<BindWechatMobileResponse> bindWechatMobile(@Valid @RequestBody BindWechatMobileRequest request) {
        BindWechatMobileResult result = authApplicationService.bindWechatMobile(BindWechatMobileCommand.builder()
            .userId(UserContextHolder.getUserId())
            .nickname(request.getNickname())
            .avatarUrl(request.getAvatarUrl())
            .phoneCode(request.getPhoneCode())
            .manualMobile(request.getManualMobile())
            .build());

        return ApiResponse.success(BindWechatMobileResponse.builder()
            .userId(result.getUserId())
            .nickname(result.getNickname())
            .avatarUrl(result.getAvatarUrl())
            .mobile(result.getMobile())
            .mobileMasked(result.getMobileMasked())
            .mobileBound(result.getMobileBound())
            .token(result.getToken())
            .refreshToken(result.getRefreshToken())
            .tokenExpireAt(result.getTokenExpireAt())
            .refreshTokenExpireAt(result.getRefreshTokenExpireAt())
            .build());
    }

    /**
     * 使用 refresh token 换发新的访问令牌。
     * 这里不要求 access token，以便前端在 access token 过期时静默续签。
     */
    @PostMapping("/refresh-token")
    public ApiResponse<RefreshAccessTokenResponse> refreshAccessToken(
        @Valid @RequestBody RefreshAccessTokenRequest request
    ) {
        RefreshAccessTokenResult result = authApplicationService.refreshAccessToken(RefreshAccessTokenCommand.builder()
            .refreshToken(request.getRefreshToken())
            .build());

        return ApiResponse.success(RefreshAccessTokenResponse.builder()
            .token(result.getToken())
            .refreshToken(result.getRefreshToken())
            .tokenExpireAt(result.getTokenExpireAt())
            .refreshTokenExpireAt(result.getRefreshTokenExpireAt())
            .build());
    }
}
