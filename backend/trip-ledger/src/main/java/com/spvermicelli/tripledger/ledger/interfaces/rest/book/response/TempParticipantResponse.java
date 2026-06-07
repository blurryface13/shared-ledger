package com.spvermicelli.tripledger.ledger.interfaces.rest.book.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TempParticipantResponse {
    private Long tempParticipantId;
    private String nickname;
    private String tempType;
    private String status;
    private Long attachedMemberId;
    private Long attachedUserId;
    private String attachedNickname;
    private String attachedAvatarUrl;
    private String attachedPhoneNumber;
}
