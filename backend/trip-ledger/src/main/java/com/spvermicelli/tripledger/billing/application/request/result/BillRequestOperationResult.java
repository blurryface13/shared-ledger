package com.spvermicelli.tripledger.billing.application.request.result;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BillRequestOperationResult {

    private Long requestId;
    private String message;
}
