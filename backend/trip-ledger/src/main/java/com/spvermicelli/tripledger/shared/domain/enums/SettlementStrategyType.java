package com.spvermicelli.tripledger.shared.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SettlementStrategyType implements BaseDbEnum {
    INTUITIVE_FIRST("INTUITIVE_FIRST"),
    MIN_TRANSFER_COUNT("MIN_TRANSFER_COUNT");

    @EnumValue
    @JsonValue
    private final String code;
}
