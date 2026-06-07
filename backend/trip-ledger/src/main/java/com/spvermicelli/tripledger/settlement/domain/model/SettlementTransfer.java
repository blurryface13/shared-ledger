package com.spvermicelli.tripledger.settlement.domain.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 结算建议转账路径。
 */
@Getter
@Builder
public class SettlementTransfer {

    private Long id;
    private Long settlementBatchId;
    private Long fromMemberId;
    private Long toMemberId;
    private Long transferAmountCent;
    private String relatedSummaryJson;
    private LocalDateTime createdAt;
}
