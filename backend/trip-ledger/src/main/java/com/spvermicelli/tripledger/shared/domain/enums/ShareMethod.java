package com.spvermicelli.tripledger.shared.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ShareMethod implements BaseDbEnum {
    AVERAGE("AVERAGE"),
    FIXED_AMOUNT("FIXED_AMOUNT"),
    RATIO("RATIO");

    @EnumValue
    @JsonValue
    private final String code;
}
