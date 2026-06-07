package com.spvermicelli.tripledger.shared.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentSourceType implements BaseDbEnum {
    SETTLEMENT_TRANSFER("SETTLEMENT_TRANSFER"),
    MANUAL("MANUAL");

    @EnumValue
    @JsonValue
    private final String code;
}
