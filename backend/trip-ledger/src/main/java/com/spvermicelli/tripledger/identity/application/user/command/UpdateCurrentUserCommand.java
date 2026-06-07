package com.spvermicelli.tripledger.identity.application.user.command;

import lombok.Builder;
import lombok.Getter;

/**
 * 更新当前用户资料命令。
 */
@Getter
@Builder
public class UpdateCurrentUserCommand {
    private Long currentUserId;
    private String nickname;
    private String avatarUrl;
    private String phoneCode;
    private String manualMobile;
}
