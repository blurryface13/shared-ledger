package com.spvermicelli.tripledger.settlement.application.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateSettlementCommand {
    private Long currentUserId;
    private Long bookId;
    private String strategyType;
}
