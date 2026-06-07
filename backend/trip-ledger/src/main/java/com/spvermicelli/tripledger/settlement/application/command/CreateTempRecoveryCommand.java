package com.spvermicelli.tripledger.settlement.application.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateTempRecoveryCommand {
    private Long currentUserId;
    private Long bookId;
    private Long tempParticipantId;
    private Long recoveryAmountCent;
    private String remark;
}
