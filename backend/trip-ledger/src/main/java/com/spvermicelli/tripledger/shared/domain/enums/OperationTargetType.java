package com.spvermicelli.tripledger.shared.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OperationTargetType implements BaseDbEnum {
    USER("USER"),
    BOOK("BOOK"),
    BOOK_MEMBER("BOOK_MEMBER"),
    BOOK_INVITATION("BOOK_INVITATION"),
    BOOK_CATEGORY("BOOK_CATEGORY"),
    TEMP_PARTICIPANT("TEMP_PARTICIPANT"),
    BILL("BILL"),
    BILL_ATTACHMENT("BILL_ATTACHMENT"),
    BILL_SHARE_ITEM("BILL_SHARE_ITEM"),
    BILL_CHANGE_REQUEST("BILL_CHANGE_REQUEST"),
    BILL_CHANGE_REQUEST_APPROVAL("BILL_CHANGE_REQUEST_APPROVAL"),
    SETTLEMENT_BATCH("SETTLEMENT_BATCH"),
    SETTLEMENT_TRANSFER("SETTLEMENT_TRANSFER"),
    PAYMENT_CONFIRM_RECORD("PAYMENT_CONFIRM_RECORD"),
    TEMP_RECOVERY_RECORD("TEMP_RECOVERY_RECORD"),
    EXPORT_RECORD("EXPORT_RECORD");

    @EnumValue
    @JsonValue
    private final String code;
}
