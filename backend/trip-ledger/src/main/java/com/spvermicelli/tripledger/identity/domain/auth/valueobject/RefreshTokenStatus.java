package com.spvermicelli.tripledger.identity.domain.auth.valueobject;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import com.spvermicelli.tripledger.shared.domain.enums.BaseDbEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * refresh token 会话状态。
 */
@Getter
@RequiredArgsConstructor
public enum RefreshTokenStatus implements BaseDbEnum {
    ACTIVE("ACTIVE"),
    USED("USED"),
    REVOKED("REVOKED"),
    EXPIRED("EXPIRED");

    @EnumValue
    @JsonValue
    private final String code;
}
