package com.spvermicelli.tripledger.identity.interfaces.rest.auth;

import com.spvermicelli.tripledger.identity.application.auth.AuthDebugApplicationService;
import com.spvermicelli.tripledger.identity.application.auth.result.MockLoginUserResult;
import com.spvermicelli.tripledger.identity.infrastructure.wechat.WechatMiniProgramProperties;
import com.spvermicelli.tripledger.identity.interfaces.rest.auth.response.MockLoginUserResponse;
import com.spvermicelli.tripledger.shared.common.enums.ErrorCode;
import com.spvermicelli.tripledger.shared.common.exception.BusinessException;
import com.spvermicelli.tripledger.shared.common.response.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/dev")
public class AuthDebugController {

    private final AuthDebugApplicationService authDebugApplicationService;
    private final WechatMiniProgramProperties wechatMiniProgramProperties;

    public AuthDebugController(
        AuthDebugApplicationService authDebugApplicationService,
        WechatMiniProgramProperties wechatMiniProgramProperties
    ) {
        this.authDebugApplicationService = authDebugApplicationService;
        this.wechatMiniProgramProperties = wechatMiniProgramProperties;
    }

    @GetMapping("/mock-users")
    public ApiResponse<List<MockLoginUserResponse>> listMockUsers() {
        if (!wechatMiniProgramProperties.isMockEnabled()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前环境未开启 mock 登录调试能力");
        }

        return ApiResponse.success(authDebugApplicationService.listMockLoginUsers()
            .stream()
            .map(this::toResponse)
            .toList());
    }

    private MockLoginUserResponse toResponse(MockLoginUserResult result) {
        return MockLoginUserResponse.builder()
            .userId(result.getUserId())
            .openId(result.getOpenId())
            .suggestedCode(result.getSuggestedCode())
            .nickname(result.getNickname())
            .mobileBound(result.getMobileBound())
            .mobileMasked(result.getMobileMasked())
            .status(result.getStatus())
            .updatedAt(result.getUpdatedAt())
            .build();
    }
}
