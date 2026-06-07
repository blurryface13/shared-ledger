package com.spvermicelli.tripledger.ledger.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.spvermicelli.tripledger.shared.domain.enums.TempParticipantStatus;
import com.spvermicelli.tripledger.shared.domain.enums.TempParticipantType;
import com.spvermicelli.tripledger.shared.infrastructure.persistence.po.base.BaseAuditPO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("tb_temp_participant")
@EqualsAndHashCode(callSuper = true)
public class TempParticipantPO extends BaseAuditPO {
    @TableField("book_id")
    private Long bookId;
    @TableField("nickname")
    private String nickname;
    @TableField("temp_type")
    private TempParticipantType tempType;
    @TableField("created_by_member_id")
    private Long createdByMemberId;
    @TableField("attached_member_id")
    private Long attachedMemberId;
    @TableField("status")
    private TempParticipantStatus status;
}
