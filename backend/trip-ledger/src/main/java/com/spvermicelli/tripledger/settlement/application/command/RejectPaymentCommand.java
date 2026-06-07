package com.spvermicelli.tripledger.settlement.application.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RejectPaymentCommand {

    private Long currentUserId;
    private Long bookId;
    private Long paymentConfirmId;
    private String rejectRemark;
}
