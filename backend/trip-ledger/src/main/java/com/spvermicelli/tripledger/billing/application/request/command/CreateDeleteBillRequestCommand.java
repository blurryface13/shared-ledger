package com.spvermicelli.tripledger.billing.application.request.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateDeleteBillRequestCommand {

    private Long currentUserId;
    private Long bookId;
    private Long billId;
    private String requestReason;
}
