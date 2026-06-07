package com.spvermicelli.tripledger.shared.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AttachmentFileType implements BaseDbEnum {
    IMAGE("IMAGE"),
    OTHER("OTHER");

    @EnumValue
    @JsonValue
    private final String code;
}
