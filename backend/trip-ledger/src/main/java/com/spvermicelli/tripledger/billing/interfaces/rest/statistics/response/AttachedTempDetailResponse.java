package com.spvermicelli.tripledger.billing.interfaces.rest.statistics.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AttachedTempDetailResponse {
    private Long tempParticipantId;
    private String tempParticipantNickname;
    private String tempParticipantType;
    private Long totalReceivableAmountCent;
    private Long sharedExpenseReceivableAmountCent;
    private Long personalCarryReceivableAmountCent;
    private Long recoveredAmountCent;
    private Long unrecoveredAmountCent;
}
