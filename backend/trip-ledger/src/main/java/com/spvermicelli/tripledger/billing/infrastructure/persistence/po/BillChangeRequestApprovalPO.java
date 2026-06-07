package com.spvermicelli.tripledger.billing.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spvermicelli.tripledger.shared.domain.enums.ApprovalAction;
import com.spvermicelli.tripledger.shared.infrastructure.persistence.po.base.BaseCreateTimePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("tb_bill_change_request_approval")
@EqualsAndHashCode(callSuper = true)
public class BillChangeRequestApprovalPO extends BaseCreateTimePO {
    @TableField("request_id")
    private Long requestId;
    @TableField("approver_member_id")
    private Long approverMemberId;
    @TableField("approval_action")
    private ApprovalAction approvalAction;
    @TableField("approval_comment")
    private String approvalComment;
}
