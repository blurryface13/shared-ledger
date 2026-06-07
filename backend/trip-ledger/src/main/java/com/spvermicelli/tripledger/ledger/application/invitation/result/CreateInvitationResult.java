package com.spvermicelli.tripledger.ledger.application.invitation.result;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateInvitationResult {
    private Long invitationId;
    private String status;
    private LocalDateTime expireAt;
    private String message;
}
