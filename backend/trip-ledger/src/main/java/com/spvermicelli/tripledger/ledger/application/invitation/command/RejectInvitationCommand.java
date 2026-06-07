package com.spvermicelli.tripledger.ledger.application.invitation.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RejectInvitationCommand {
    private Long currentUserId;
    private Long invitationId;
}
