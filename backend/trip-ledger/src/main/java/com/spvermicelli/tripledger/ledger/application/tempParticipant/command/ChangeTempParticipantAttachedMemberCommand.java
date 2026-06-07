package com.spvermicelli.tripledger.ledger.application.tempParticipant.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChangeTempParticipantAttachedMemberCommand {
    private Long currentUserId;
    private Long bookId;
    private Long tempParticipantId;
    private Long attachedMemberId;
}
