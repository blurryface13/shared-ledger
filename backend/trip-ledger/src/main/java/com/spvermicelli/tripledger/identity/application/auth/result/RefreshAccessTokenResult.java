package com.spvermicelli.tripledger.identity.application.auth.result;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

/**
 * refresh token 续签结果。
 */
@Getter
@Builder
public class RefreshAccessTokenResult {
    private String token;
    private String refreshToken;
    private Instant tokenExpireAt;
    private Instant refreshTokenExpireAt;
}
