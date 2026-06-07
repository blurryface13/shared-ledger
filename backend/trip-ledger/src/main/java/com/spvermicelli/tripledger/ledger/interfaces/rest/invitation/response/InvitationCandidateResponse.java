package com.spvermicelli.tripledger.ledger.interfaces.rest.invitation.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InvitationCandidateResponse {
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String phoneNumber;
    private boolean pendingInvitation;
}
