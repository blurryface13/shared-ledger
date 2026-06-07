package com.spvermicelli.tripledger.billing.interfaces.rest.request.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BillRequestOperationResponse {

    private Long requestId;
    private String message;
}
