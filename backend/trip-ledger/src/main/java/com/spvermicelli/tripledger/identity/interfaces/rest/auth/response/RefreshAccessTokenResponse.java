package com.spvermicelli.tripledger.identity.interfaces.rest.auth.response;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

/**
 * refresh token 续签响应。
 */
@Getter
@Builder
public class RefreshAccessTokenResponse {
    private String token;
    private String refreshToken;
    private Instant tokenExpireAt;
    private Instant refreshTokenExpireAt;
}
