package com.spvermicelli.tripledger.billing.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spvermicelli.tripledger.shared.domain.enums.ChangeRequestStatus;
import com.spvermicelli.tripledger.shared.domain.enums.ChangeRequestType;
import com.spvermicelli.tripledger.shared.infrastructure.persistence.po.base.BaseIdPO;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("tb_bill_change_request")
@EqualsAndHashCode(callSuper = true)
public class BillChangeRequestPO extends BaseIdPO {
    @TableField("bill_id")
    private Long billId;
    @TableField("predecessor_request_id")
    private Long predecessorRequestId;
    @TableField("is_baseline")
    private Boolean baseline;
    @TableField("request_type")
    private ChangeRequestType requestType;
    @TableField("requester_member_id")
    private Long requesterMemberId;
    @TableField("request_reason")
    private String requestReason;
    @TableField("status")
    private ChangeRequestStatus status;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("handled_at")
    private LocalDateTime handledAt;
}
