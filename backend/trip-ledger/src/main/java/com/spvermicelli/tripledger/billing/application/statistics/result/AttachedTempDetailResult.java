package com.spvermicelli.tripledger.billing.application.statistics.result;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AttachedTempDetailResult {
    private Long tempParticipantId;
    private String tempParticipantNickname;
    private String tempParticipantType;
    private Long totalReceivableAmountCent;
    private Long sharedExpenseReceivableAmountCent;
    private Long personalCarryReceivableAmountCent;
    private Long recoveredAmountCent;
    private Long unrecoveredAmountCent;
}
