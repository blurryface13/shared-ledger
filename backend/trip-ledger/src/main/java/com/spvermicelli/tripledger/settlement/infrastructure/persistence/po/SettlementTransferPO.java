package com.spvermicelli.tripledger.settlement.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spvermicelli.tripledger.shared.infrastructure.persistence.po.base.BaseCreateTimePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("tb_settlement_transfer")
@EqualsAndHashCode(callSuper = true)
public class SettlementTransferPO extends BaseCreateTimePO {
    @TableField("settlement_batch_id")
    private Long settlementBatchId;
    @TableField("from_member_id")
    private Long fromMemberId;
    @TableField("to_member_id")
    private Long toMemberId;
    @TableField("transfer_amount_cent")
    private Long transferAmountCent;
    @TableField("related_summary_json")
    private String relatedSummaryJson;
}
