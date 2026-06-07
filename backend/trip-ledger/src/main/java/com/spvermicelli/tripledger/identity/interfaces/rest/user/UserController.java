package com.spvermicelli.tripledger.identity.interfaces.rest.user;

import com.spvermicelli.tripledger.identity.application.user.UserApplicationService;
import com.spvermicelli.tripledger.identity.application.user.command.CancelCurrentUserCommand;
import com.spvermicelli.tripledger.identity.application.user.command.UpdateCurrentUserCommand;
import com.spvermicelli.tripledger.identity.application.user.result.CurrentUserResult;
import com.spvermicelli.tripledger.identity.interfaces.rest.user.request.UpdateCurrentUserRequest;
import com.spvermicelli.tripledger.identity.interfaces.rest.user.response.CurrentUserResponse;
import com.spvermicelli.tripledger.shared.common.context.UserContextHolder;
import com.spvermicelli.tripledger.shared.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前用户接口。
 * 所有操作都默认作用于当前登录用户本人。
 */
@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {

    private final UserApplicationService userApplicationService;

    public UserController(UserApplicationService userApplicationService) {
        this.userApplicationService = userApplicationService;
    }

    /**
     * 查询当前用户信息。
     * 该接口只返回当前登录用户本人的资料摘要。
     */
    @GetMapping
    public ApiResponse<CurrentUserResponse> getCurrentUser() {
        CurrentUserResult result = userApplicationService.getCurrentUser(UserContextHolder.getUserId());
        return ApiResponse.success(toResponse(result));
    }

    /**
     * 更新当前用户信息。
     */
    @PutMapping
    public ApiResponse<CurrentUserResponse> updateCurrentUser(@Valid @RequestBody UpdateCurrentUserRequest request) {
        CurrentUserResult result = userApplicationService.updateCurrentUser(UpdateCurrentUserCommand.builder()
            .currentUserId(UserContextHolder.getUserId())
            .nickname(request.getNickname())
            .avatarUrl(request.getAvatarUrl())
            .phoneCode(request.getPhoneCode())
            .manualMobile(request.getManualMobile())
            .build());

        return ApiResponse.success(toResponse(result));
    }

    @PostMapping("/cancel")
    public ApiResponse<Void> cancelCurrentUser() {
        userApplicationService.cancelCurrentUser(CancelCurrentUserCommand.builder()
            .currentUserId(UserContextHolder.getUserId())
            .build());
        return ApiResponse.success();
    }

    private CurrentUserResponse toResponse(CurrentUserResult result) {
        return CurrentUserResponse.builder()
            .userId(result.getUserId())
            .nickname(result.getNickname())
            .avatarUrl(result.getAvatarUrl())
            .mobile(result.getMobile())
            .mobileMasked(result.getMobileMasked())
            .mobileBound(result.getMobileBound())
            .status(result.getStatus())
            .createdAt(result.getCreatedAt())
            .updatedAt(result.getUpdatedAt())
            .build();
    }
}
