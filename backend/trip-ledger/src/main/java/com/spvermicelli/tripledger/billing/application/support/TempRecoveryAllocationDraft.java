package com.spvermicelli.tripledger.billing.application.support;

import lombok.Builder;
import lombok.Getter;

/**
 * 临时成员回补自动分配草稿。
 */
@Getter
@Builder
public class TempRecoveryAllocationDraft {

    private Long billId;
    private Long allocatedAmountCent;
}
