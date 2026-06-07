package com.spvermicelli.tripledger.settlement.application.result;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TempRecoveryResult {
    private Long recoveryRecordId;
    private Long bookId;
    private Long tempParticipantId;
    private String tempParticipantNickname;
    private Long attachedMemberId;
    private Long recoveryAmountCent;
    private String confirmStatus;
    private Long confirmedByMemberId;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private String remark;
    private List<TempRecoveryAllocationResult> allocationList;
}
