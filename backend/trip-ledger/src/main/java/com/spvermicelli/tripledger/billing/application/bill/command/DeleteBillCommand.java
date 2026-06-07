package com.spvermicelli.tripledger.billing.application.bill.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeleteBillCommand {

    private Long currentUserId;
    private Long bookId;
    private Long billId;
    private boolean fromChangeRequest;
}
