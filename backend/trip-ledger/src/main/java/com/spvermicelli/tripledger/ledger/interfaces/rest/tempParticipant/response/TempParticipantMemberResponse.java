package com.spvermicelli.tripledger.ledger.interfaces.rest.tempParticipant.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TempParticipantMemberResponse {
    private Long memberId;
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String phoneNumber;
}
