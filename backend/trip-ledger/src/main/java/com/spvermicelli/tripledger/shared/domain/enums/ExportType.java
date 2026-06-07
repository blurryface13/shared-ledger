package com.spvermicelli.tripledger.shared.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExportType implements BaseDbEnum {
    PERSONAL_DETAIL("PERSONAL_DETAIL"),
    BOOK_SUMMARY("BOOK_SUMMARY");

    @EnumValue
    @JsonValue
    private final String code;
}
