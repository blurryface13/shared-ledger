package com.spvermicelli.tripledger.ledger.interfaces.rest.invitation.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateInvitationResponse {
    private Long invitationId;
    private String status;
    private LocalDateTime expireAt;
    private String message;
}
