package com.spvermicelli.tripledger.billing.application.bill.result;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BillOperationResult {

    private Long billId;
    private String message;
}
