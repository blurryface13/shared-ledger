package com.spvermicelli.tripledger.settlement.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spvermicelli.tripledger.shared.infrastructure.persistence.po.base.BaseCreateTimePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("tb_temp_recovery_allocation")
@EqualsAndHashCode(callSuper = true)
public class TempRecoveryAllocationPO extends BaseCreateTimePO {
    @TableField("recovery_record_id")
    private Long recoveryRecordId;
    @TableField("bill_id")
    private Long billId;
    @TableField("allocated_amount_cent")
    private Long allocatedAmountCent;
}
