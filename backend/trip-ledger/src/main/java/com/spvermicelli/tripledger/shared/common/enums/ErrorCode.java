package com.spvermicelli.tripledger.shared.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    SUCCESS(0, "success"),
    INVALID_PARAM(4001, "参数错误"),
    UNAUTHORIZED(4002, "未登录或登录已失效"),
    FORBIDDEN(4003, "无权限访问"),
    NOT_FOUND(4004, "对象不存在"),
    INVISIBLE(4005, "对象不可见"),
    INVALID_STATUS(4006, "当前状态不允许该操作"),
    CONFLICT(4007, "数据冲突"),
    TOKEN_TYPE_INVALID(4008, "当前 token 不具备该操作权限"),
    SYSTEM_ERROR(5000, "系统异常");

    private final int code;
    private final String defaultMessage;
}
