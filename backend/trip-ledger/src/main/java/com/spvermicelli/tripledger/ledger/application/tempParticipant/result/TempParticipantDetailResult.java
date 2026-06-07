package com.spvermicelli.tripledger.ledger.application.tempParticipant.result;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TempParticipantDetailResult {
    private Long tempParticipantId;
    private String nickname;
    private String tempType;
    private String status;
    private TempParticipantMemberResult createdByMember;
    private TempParticipantMemberResult attachedMember;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
