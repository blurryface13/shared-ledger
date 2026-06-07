package com.spvermicelli.tripledger.settlement.application.result;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentConfirmAllocationResult {
    private Long billId;
    private Long allocatedAmountCent;
}
