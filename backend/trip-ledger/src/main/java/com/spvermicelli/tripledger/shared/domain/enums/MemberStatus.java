package com.spvermicelli.tripledger.shared.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberStatus implements BaseDbEnum {
    ACTIVE("ACTIVE"),
    QUIT("QUIT"),
    REMOVED("REMOVED");

    @EnumValue
    @JsonValue
    private final String code;
}
