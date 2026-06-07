package com.spvermicelli.tripledger.shared.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ShareParticipantType implements BaseDbEnum {
    MEMBER("MEMBER"),
    TEMP_PARTICIPANT("TEMP_PARTICIPANT");

    @EnumValue
    @JsonValue
    private final String code;
}
