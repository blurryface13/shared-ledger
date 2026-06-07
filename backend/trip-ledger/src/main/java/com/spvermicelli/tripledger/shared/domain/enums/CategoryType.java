package com.spvermicelli.tripledger.shared.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 分类对应的账务方向。
 * 当前只区分收入和支出，后续账单模块按该枚举决定可选分类范围。
 */
@Getter
@RequiredArgsConstructor
public enum CategoryType implements BaseDbEnum {
    EXPENSE("EXPENSE"),
    INCOME("INCOME");

    @EnumValue
    @JsonValue
    private final String code;
}
