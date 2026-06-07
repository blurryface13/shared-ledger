package com.spvermicelli.tripledger.billing.application.support;

import lombok.Builder;
import lombok.Getter;

/**
 * 临时成员对正式成员形成的一条内部回补责任线。
 * 该责任既可能来自个人帮带，也可能来自共享账单里的临时成员分摊。
 */
@Getter
@Builder
public class TempRecoveryLine {

    private Long billId;
    private Long tempParticipantId;
    private Long receiverMemberId;
    private Long amountCent;
}
