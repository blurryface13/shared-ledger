package com.spvermicelli.tripledger.ledger.interfaces.rest.tempParticipant.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TempParticipantOperationResponse {
    private Long tempParticipantId;
    private String message;
}
