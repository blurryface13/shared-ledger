package com.spvermicelli.tripledger.billing.interfaces.rest.bill.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BillOperationResponse {

    private Long billId;
    private String message;
}
