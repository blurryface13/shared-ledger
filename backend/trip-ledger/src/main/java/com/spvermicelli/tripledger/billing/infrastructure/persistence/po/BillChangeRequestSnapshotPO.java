package com.spvermicelli.tripledger.billing.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spvermicelli.tripledger.shared.infrastructure.persistence.po.base.BaseCreateTimePO;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("tb_bill_change_request_snapshot")
@EqualsAndHashCode(callSuper = true)
public class BillChangeRequestSnapshotPO extends BaseCreateTimePO {
    @TableField("request_id")
    private Long requestId;
    @TableField("bill_type")
    private String billType;
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
    @TableField("temp_participant_id")
    private Long tempParticipantId;
    @TableField("bill_time")
    private LocalDateTime billTime;
    @TableField("remark")
    private String remark;
}
