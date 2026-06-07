package com.spvermicelli.tripledger.ledger.application.invitation.result;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InvitationOperationResult {
    private Long invitationId;
    private String status;
    private String message;
}
