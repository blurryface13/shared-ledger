package com.spvermicelli.tripledger.billing.application.bill.result;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@lombok.extern.jackson.Jacksonized
public class BillOperationResult {

    private Long billId;
    private String message;
}
