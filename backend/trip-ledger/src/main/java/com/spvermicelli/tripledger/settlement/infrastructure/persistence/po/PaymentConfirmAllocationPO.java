package com.spvermicelli.tripledger.settlement.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spvermicelli.tripledger.shared.infrastructure.persistence.po.base.BaseCreateTimePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("tb_payment_confirm_allocation")
@EqualsAndHashCode(callSuper = true)
public class PaymentConfirmAllocationPO extends BaseCreateTimePO {
    @TableField("payment_confirm_id")
    private Long paymentConfirmId;
    @TableField("bill_id")
    private Long billId;
    @TableField("allocated_amount_cent")
    private Long allocatedAmountCent;
}
