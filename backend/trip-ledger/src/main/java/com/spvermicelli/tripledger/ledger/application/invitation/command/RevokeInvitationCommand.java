package com.spvermicelli.tripledger.ledger.application.invitation.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RevokeInvitationCommand {
    private Long currentUserId;
    private Long invitationId;
}
