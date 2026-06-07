package com.spvermicelli.tripledger.billing.application.support;

import lombok.Builder;
import lombok.Getter;

/**
 * 共享账单形成的一条正式成员间债务线。
 * 当分摊对象是临时成员时，会被归并到其挂靠正式成员身上参与对外结算。
 */
@Getter
@Builder
public class SharedDebtLine {

    private Long billId;
    private Long fromMemberId;
    private Long toMemberId;
    private Long sourceTempParticipantId;
    private Long amountCent;
}
