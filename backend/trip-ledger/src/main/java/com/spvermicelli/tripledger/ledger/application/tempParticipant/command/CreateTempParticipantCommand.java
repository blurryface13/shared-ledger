package com.spvermicelli.tripledger.ledger.application.tempParticipant.command;

import com.spvermicelli.tripledger.shared.domain.enums.TempParticipantType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateTempParticipantCommand {
    private Long currentUserId;
    private Long bookId;
    private String nickname;
    private TempParticipantType tempType;
    private Long attachedMemberId;
}
