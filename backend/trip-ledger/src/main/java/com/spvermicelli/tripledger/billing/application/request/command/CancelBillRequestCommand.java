package com.spvermicelli.tripledger.billing.application.request.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CancelBillRequestCommand {

    private Long currentUserId;
    private Long bookId;
    private Long requestId;
}

