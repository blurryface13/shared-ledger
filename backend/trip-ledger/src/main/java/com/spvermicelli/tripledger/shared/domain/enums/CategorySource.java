package com.spvermicelli.tripledger.shared.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 分类来源枚举。
 * SYSTEM 由系统初始化；
 * CUSTOM_GLOBAL 为账本级分类（账本成员可见，需 owner/admin 维护）；
 * CUSTOM_LOCAL 为成员个人可见分类（仅创建者本人可见与维护）。
 */
@Getter
@RequiredArgsConstructor
public enum CategorySource implements BaseDbEnum {
    SYSTEM("SYSTEM"),
    CUSTOM_GLOBAL("CUSTOM_GLOBAL"),
    CUSTOM_LOCAL("CUSTOM_LOCAL");

    @EnumValue
    @JsonValue
    private final String code;
}
