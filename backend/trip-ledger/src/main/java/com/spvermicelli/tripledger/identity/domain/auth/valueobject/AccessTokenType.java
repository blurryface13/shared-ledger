package com.spvermicelli.tripledger.identity.domain.auth.valueobject;

/**
 * 访问型 JWT 的业务类型。
 */
public enum AccessTokenType {
    /**
     * 正式业务访问令牌。
     */
    ACCESS,

    /**
     * 首登阶段绑定手机号的临时令牌。
     */
    BIND_MOBILE
}
