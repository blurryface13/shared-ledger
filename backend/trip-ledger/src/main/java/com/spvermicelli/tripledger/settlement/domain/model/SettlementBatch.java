package com.spvermicelli.tripledger.settlement.domain.model;

import com.spvermicelli.tripledger.shared.domain.enums.SettlementScopeType;
import com.spvermicelli.tripledger.shared.domain.enums.SettlementStatus;
import com.spvermicelli.tripledger.shared.domain.enums.SettlementStrategyType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 结算批次聚合根。
 * 结算是“当前时刻的快照结果”，不会冻结账单，只保留历史计算输出。
 */
@Getter
@Builder
public class SettlementBatch {

    private Long id;
    private Long bookId;
    private Long initiatorMemberId;
    private SettlementStrategyType strategyType;
    private SettlementScopeType scopeType;
    private SettlementStatus status;
    private LocalDateTime snapshotTime;
    private LocalDateTime createdAt;
}
