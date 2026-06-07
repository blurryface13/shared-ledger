package com.spvermicelli.tripledger.ledger.application.invitation.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateInvitationCommand {
    private Long currentUserId;
    private Long bookId;
    private Long inviteeUserId;
    private String remark;
}
