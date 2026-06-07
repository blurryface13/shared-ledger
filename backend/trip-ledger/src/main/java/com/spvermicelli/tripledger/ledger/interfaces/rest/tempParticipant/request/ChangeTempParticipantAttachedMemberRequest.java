package com.spvermicelli.tripledger.ledger.interfaces.rest.tempParticipant.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeTempParticipantAttachedMemberRequest {
    private Long attachedMemberId;
}
