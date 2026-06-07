package com.spvermicelli.tripledger.ledger.application.book.result;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TempParticipantResult {
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
