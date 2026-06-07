package com.spvermicelli.tripledger.shared.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserStatus implements BaseDbEnum {
    ACTIVE("ACTIVE"),
    DISABLED("DISABLED"),
    CANCELLED("CANCELLED");

    @EnumValue
    @JsonValue
    private final String code;
}
