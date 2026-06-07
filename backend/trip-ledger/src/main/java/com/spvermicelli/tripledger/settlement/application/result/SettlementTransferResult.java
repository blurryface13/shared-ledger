package com.spvermicelli.tripledger.settlement.application.result;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SettlementTransferResult {
    private Long transferId;
    private Long fromMemberId;
    private String fromMemberName;
    private Long toMemberId;
    private String toMemberName;
    private Long transferAmountCent;
    private String relatedSummaryJson;
}
