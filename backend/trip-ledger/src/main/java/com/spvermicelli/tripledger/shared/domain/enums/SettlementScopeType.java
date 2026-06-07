package com.spvermicelli.tripledger.shared.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SettlementScopeType implements BaseDbEnum {
    PERSONAL_VIEW("PERSONAL_VIEW");

    @EnumValue
    @JsonValue
    private final String code;
}
