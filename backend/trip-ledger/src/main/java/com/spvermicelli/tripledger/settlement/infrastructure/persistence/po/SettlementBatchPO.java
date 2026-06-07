package com.spvermicelli.tripledger.settlement.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spvermicelli.tripledger.shared.domain.enums.SettlementScopeType;
import com.spvermicelli.tripledger.shared.domain.enums.SettlementStatus;
import com.spvermicelli.tripledger.shared.domain.enums.SettlementStrategyType;
import com.spvermicelli.tripledger.shared.infrastructure.persistence.po.base.BaseCreateTimePO;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("tb_settlement_batch")
@EqualsAndHashCode(callSuper = true)
public class SettlementBatchPO extends BaseCreateTimePO {
    @TableField("book_id")
    private Long bookId;
    @TableField("initiator_member_id")
    private Long initiatorMemberId;
    @TableField("strategy_type")
    private SettlementStrategyType strategyType;
    @TableField("scope_type")
    private SettlementScopeType scopeType;
    @TableField("status")
    private SettlementStatus status;
    @TableField("snapshot_time")
    private LocalDateTime snapshotTime;
}
