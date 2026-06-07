package com.spvermicelli.tripledger.ledger.interfaces.rest.invitation.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InvitationOperationResponse {
    private Long invitationId;
    private String status;
    private String message;
}
