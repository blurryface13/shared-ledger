package com.spvermicelli.tripledger.ledger.application.invitation.result;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InvitationCandidateResult {
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String phoneNumber;
    private boolean pendingInvitation;
}
