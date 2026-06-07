package com.spvermicelli.tripledger.shared.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BillChangeFlowStatus implements BaseDbEnum {
    NONE("NONE"),
    PENDING("PENDING"),
    HISTORY("HISTORY");

    @EnumValue
    @JsonValue
    private final String code;
}
