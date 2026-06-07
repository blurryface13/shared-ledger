package com.spvermicelli.tripledger.billing.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spvermicelli.tripledger.shared.domain.enums.BillStatus;
import com.spvermicelli.tripledger.shared.domain.enums.BillChangeFlowStatus;
import com.spvermicelli.tripledger.shared.domain.enums.BillType;
import com.spvermicelli.tripledger.shared.infrastructure.persistence.po.base.BaseAuditPO;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("tb_bill")
@EqualsAndHashCode(callSuper = true)
public class BillPO extends BaseAuditPO {
    @TableField("book_id")
    private Long bookId;
    @TableField("bill_type")
    private BillType billType;
    @TableField("title")
    private String title;
    @TableField("bill_amount_cent")
    private Long billAmountCent;
    @TableField("category_id")
    private Long categoryId;
    @TableField("payer_member_id")
    private Long payerMemberId;
    @TableField("recorder_member_id")
    private Long recorderMemberId;
    @TableField("target_temp_participant_id")
    private Long targetTempParticipantId;
    @TableField("bill_time")
    private LocalDateTime billTime;
    @TableField("remark")
    private String remark;
    @TableField("change_flow_status")
    private BillChangeFlowStatus changeFlowStatus;
    @TableField("latest_change_request_id")
    private Long latestChangeRequestId;
    @TableField("has_change_history")
    private Boolean hasChangeHistory;
    @TableField("status")
    private BillStatus status;
}
