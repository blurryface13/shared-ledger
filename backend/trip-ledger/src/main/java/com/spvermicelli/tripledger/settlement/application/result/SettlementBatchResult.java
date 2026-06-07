package com.spvermicelli.tripledger.settlement.application.result;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SettlementBatchResult {
    private Long settlementBatchId;
    private Long bookId;
    private Long initiatorMemberId;
    private String strategyType;
    private String scopeType;
    private String status;
    private LocalDateTime snapshotTime;
    private LocalDateTime createdAt;
    private List<SettlementTransferResult> transferList;
}
