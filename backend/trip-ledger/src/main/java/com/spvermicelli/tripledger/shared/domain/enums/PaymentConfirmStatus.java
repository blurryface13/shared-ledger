package com.spvermicelli.tripledger.shared.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentConfirmStatus implements BaseDbEnum {
    PENDING_CONFIRM("PENDING_CONFIRM"),
    CONFIRMED("CONFIRMED"),
    AUTO_CONFIRMED("AUTO_CONFIRMED"),
    REJECT("REJECT");

    @EnumValue
    @JsonValue
    private final String code;
}
