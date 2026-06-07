package com.spvermicelli.tripledger.identity.application.auth.command;

import lombok.Builder;
import lombok.Getter;

/**
 * refresh token 换发访问令牌命令。
 */
@Getter
@Builder
public class RefreshAccessTokenCommand {
    private String refreshToken;
}
