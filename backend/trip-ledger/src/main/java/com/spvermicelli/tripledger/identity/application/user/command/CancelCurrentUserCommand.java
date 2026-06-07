package com.spvermicelli.tripledger.identity.application.user.command;

import lombok.Builder;
import lombok.Getter;

/**
 * 注销当前用户命令。
 */
@Getter
@Builder
public class CancelCurrentUserCommand {
    private Long currentUserId;
}
