package com.spvermicelli.tripledger.shared.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BillStatus implements BaseDbEnum {
    ACTIVE("ACTIVE"),
    DELETED("DELETED");

    @EnumValue
    @JsonValue
    private final String code;
}
