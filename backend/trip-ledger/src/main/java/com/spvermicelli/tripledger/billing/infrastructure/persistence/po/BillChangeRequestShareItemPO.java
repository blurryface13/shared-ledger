package com.spvermicelli.tripledger.billing.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spvermicelli.tripledger.shared.domain.enums.ShareMethod;
import com.spvermicelli.tripledger.shared.domain.enums.ShareParticipantType;
import com.spvermicelli.tripledger.shared.infrastructure.persistence.po.base.BaseCreateTimePO;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("tb_bill_change_request_share_item")
@EqualsAndHashCode(callSuper = true)
public class BillChangeRequestShareItemPO extends BaseCreateTimePO {
    @TableField("request_id")
    private Long requestId;
    @TableField("participant_type")
    private ShareParticipantType participantType;
    @TableField("participant_ref_id")
    private Long participantRefId;
    @TableField("attached_member_id")
    private Long attachedMemberId;
    @TableField("share_method")
    private ShareMethod shareMethod;
    @TableField("share_ratio")
    private BigDecimal shareRatio;
    @TableField("share_amount_cent")
    private Long shareAmountCent;
}
