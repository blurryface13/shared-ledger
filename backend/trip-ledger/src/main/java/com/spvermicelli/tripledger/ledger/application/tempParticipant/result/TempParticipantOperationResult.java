package com.spvermicelli.tripledger.ledger.application.tempParticipant.result;

import lombok.Builder;
import lombok.Getter;

/**
 * 临时成员写操作结果。
 * 统一保留 message 字段，方便前端直接展示更贴近业务语义的提示。
 */
@Getter
@Builder
public class TempParticipantOperationResult {
    private Long tempParticipantId;
    private String message;
}
