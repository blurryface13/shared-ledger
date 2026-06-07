package com.spvermicelli.tripledger.settlement.application.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateDirectReceiveCommand {
    private Long currentUserId;
    private Long bookId;
    private Long fromMemberId;
    private Long paymentAmountCent;
    private String sourceType;
    private Long sourceRefId;
    private String remark;
}
