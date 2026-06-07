package com.spvermicelli.tripledger.settlement.interfaces.rest.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentConfirmAllocationResponse {
    private Long billId;
    private Long allocatedAmountCent;
}
