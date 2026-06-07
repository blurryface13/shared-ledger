package com.spvermicelli.tripledger.shared.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BillType implements BaseDbEnum {
    PERSONAL_EXPENSE("PERSONAL_EXPENSE"),
    PERSONAL_INCOME("PERSONAL_INCOME"),
    PERSONAL_CARRY("PERSONAL_CARRY"),
    SHARED_EXPENSE("SHARED_EXPENSE");

    @EnumValue
    @JsonValue
    private final String code;
}
