package com.spvermicelli.tripledger.settlement.application.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreatePaymentConfirmCommand {
    private Long currentUserId;
    private Long bookId;
    private Long toMemberId;
    private Long paymentAmountCent;
    private String sourceType;
    private Long sourceRefId;
    private String remark;
}
