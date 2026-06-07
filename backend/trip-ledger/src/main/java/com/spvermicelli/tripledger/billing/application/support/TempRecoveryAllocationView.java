package com.spvermicelli.tripledger.billing.application.support;

import lombok.Builder;
import lombok.Getter;

/**
 * 临时成员回补分配视图。
 * 该视图把回补主记录和账单分配组合起来，便于单账单已收回金额计算。
 */
@Getter
@Builder
public class TempRecoveryAllocationView {

    private Long recoveryRecordId;
    private Long billId;
    private Long tempParticipantId;
    private Long attachedMemberId;
    private Long allocatedAmountCent;
}
