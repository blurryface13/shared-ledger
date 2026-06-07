package com.spvermicelli.tripledger.settlement.application.result;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TempRecoveryAllocationResult {
    private Long billId;
    private Long allocatedAmountCent;
}
