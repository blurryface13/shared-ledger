package com.spvermicelli.tripledger.shared.common.context;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginUser {
    /**
     * 当前登录用户 ID。
     */
    private Long userId;

    /**
     * 当前请求携带的 JWT 类型。
     * ACCESS 表示正式访问令牌。
     * BIND_MOBILE 表示仅可用于绑定手机号的临时令牌。
     */
    private String tokenType;
}
