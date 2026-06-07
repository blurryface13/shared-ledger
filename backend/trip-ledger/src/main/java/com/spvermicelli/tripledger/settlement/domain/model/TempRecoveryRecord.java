package com.spvermicelli.tripledger.settlement.domain.model;

import com.spvermicelli.tripledger.shared.domain.enums.RecoveryConfirmStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 临时成员回补记录。
 * 该记录描述的是“临时成员 -> 挂靠正式成员”的内部回补关系，
 * 与正式成员之间的对外补款记录分开管理。
 */
@Getter
@Builder
public class TempRecoveryRecord {

    private Long id;
    private Long bookId;
    private Long tempParticipantId;
    private Long attachedMemberId;
    private Long recoveryAmountCent;
    private RecoveryConfirmStatus confirmStatus;
    private Long confirmedByMemberId;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private String remark;
}
