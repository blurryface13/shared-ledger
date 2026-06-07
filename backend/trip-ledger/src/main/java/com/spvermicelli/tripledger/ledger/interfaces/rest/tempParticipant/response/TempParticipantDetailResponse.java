package com.spvermicelli.tripledger.ledger.interfaces.rest.tempParticipant.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 临时成员列表返回对象。
 */
@Getter
@Builder
public class TempParticipantDetailResponse {
    private Long tempParticipantId;
    private String nickname;
    private String tempType;
    private String status;
    private TempParticipantMemberResponse createdByMember;
    private TempParticipantMemberResponse attachedMember;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
